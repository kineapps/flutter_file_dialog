import { FlutterPlugin, FlutterPluginBinding, MethodCall, MethodCallHandler,
  MethodChannel, MethodResult } from '@ohos/flutter_ohos';
import AbilityAware from '@ohos/flutter_ohos/src/main/ets/embedding/engine/plugins/ability/AbilityAware';
import { AbilityPluginBinding } from '@ohos/flutter_ohos/src/main/ets/embedding/engine/plugins/ability/AbilityPluginBinding';
import { common } from '@kit.AbilityKit';
import { picker, fileIo as fs } from '@kit.CoreFileKit';
import { BusinessError } from '@kit.BasicServicesKit';

/** Document picker grants are URI based; all public-file IO stays native. */
export default class FlutterFileDialogPlugin implements FlutterPlugin, MethodCallHandler, AbilityAware {
  private channel: MethodChannel | null = null;
  private context: common.UIAbilityContext | null = null;
  private busy: boolean = false;

  getUniqueClassName(): string { return 'FlutterFileDialogPlugin'; }
  onAttachedToAbility(binding: AbilityPluginBinding): void { this.context = binding.getAbility().context; }
  onDetachedFromAbility(): void { this.context = null; }

  onAttachedToEngine(binding: FlutterPluginBinding): void {
    this.channel = new MethodChannel(binding.getBinaryMessenger(), 'flutter_file_dialog');
    this.channel.setMethodCallHandler(this);
  }

  onDetachedFromEngine(binding: FlutterPluginBinding): void {
    this.channel?.setMethodCallHandler(null);
    this.channel = null;
    this.context = null;
  }

  async onMethodCall(call: MethodCall, result: MethodResult): Promise<void> {
    if (call.method === 'isPickDirectorySupported') { result.success(false); return; }
    if (call.method !== 'pickFile' && call.method !== 'saveFile') { result.notImplemented(); return; }
    const context = this.context;
    if (context === null) { result.error('no_activity', 'A foreground UIAbility is required.', null); return; }
    if (this.busy) { result.error('dialog_busy', 'A file dialog is already open.', null); return; }
    this.busy = true;
    try {
      const args = call.args as Map<string, Object>;
      const documentPicker = new picker.DocumentViewPicker(context);
      if (call.method === 'pickFile') {
        const options = new picker.DocumentSelectOptions();
        options.maxSelectNumber = 1;
        const extensions = args?.get('fileExtensionsFilter') as string[] | undefined;
        if (extensions !== undefined && extensions !== null && extensions.length > 0) {
          options.fileSuffixFilters = extensions.map((extension: string) =>
            extension.startsWith('.') ? extension : '.' + extension);
        }
        const uris = await documentPicker.select(options);
        if (uris.length === 0) { result.success(null); return; }
        if (args?.get('copyFileToCacheDir') === false) { result.success(uris[0]); return; }
        const source = fs.openSync(uris[0], fs.OpenMode.READ_ONLY);
        try {
          const folder = fs.mkdtempSync(context.cacheDir + '/file-dialog-XXXXXX');
          const name = source.name.replace(/[/\\]/g, '_');
          const destination = folder + '/' + name;
          fs.copyFileSync(source.fd, destination);
          result.success(destination);
        } finally { fs.closeSync(source); }
      } else {
        const sourcePath = args.get('sourceFilePath') as string | undefined;
        const data = args.get('data') as Uint8Array | undefined;
        const suggestedName = args.get('fileName') as string | undefined;
        if ((data === undefined || data === null) && (sourcePath === undefined || sourcePath === null)) {
          result.error('invalid_arguments', 'Provide data or sourceFilePath.', null);
          return;
        }
        const options = new picker.DocumentSaveOptions();
        options.newFileNames = [suggestedName || sourcePath?.split('/').pop() || 'file'];
        const uris = await documentPicker.save(options);
        if (uris.length === 0) { result.success(null); return; }
        const destination = fs.openSync(uris[0], fs.OpenMode.READ_WRITE | fs.OpenMode.CREATE | fs.OpenMode.TRUNC);
        try {
          if (data !== undefined && data !== null) {
            let offset = 0;
            while (offset < data.length) {
              const written = fs.writeSync(destination.fd, data.slice(offset).buffer);
              if (written <= 0) throw new Error('File write made no progress');
              offset += written;
            }
          } else {
            const source = fs.openSync(sourcePath as string, fs.OpenMode.READ_ONLY);
            try { fs.copyFileSync(source.fd, destination.fd); } finally { fs.closeSync(source); }
          }
          fs.fsyncSync(destination.fd);
        } finally { fs.closeSync(destination); }
        result.success(uris[0]);
      }
    } catch (error) {
      result.error('file_dialog_error', 'The file operation failed.', (error as BusinessError).code);
    } finally { this.busy = false; }
  }
}
