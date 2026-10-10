package dtm.stools.component.panels.editor.powerpoint.ui.popup;

import dtm.stools.component.inputfields.osfilepicker.DeFilter;
import dtm.stools.component.inputfields.osfilepicker.OsFilePicker;
import dtm.stools.component.panels.editor.powerpoint.model.PptObject;
import dtm.stools.component.panels.editor.powerpoint.provider.PowerPointFileDialogProvider;
import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class DefaultPowerPointFileDialogProvider implements PowerPointFileDialogProvider {
    @Override
    public String id(){return "powerpoint.files.default";}
    @Override
    public Optional<Path> chooseOpen(Component owner){
        return Optional.ofNullable(OsFilePicker.openFile("Abrir apresentação",DeFilter.of("PowerPoint","pptx"))).map(File::toPath);
    }
    @Override
    public Optional<Path> chooseSave(Component owner,Path current){
        File file=OsFilePicker.saveFile("Salvar apresentação",current==null?null:current.toAbsolutePath().getParent().toFile(),
                current==null?"Apresentação.pptx":current.getFileName().toString(),DeFilter.of("PowerPoint","pptx"));
        if(file==null)return Optional.empty();
        Path path=file.toPath();
        if(!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pptx"))path=path.resolveSibling(path.getFileName()+".pptx");
        return Optional.of(path);
    }
    @Override
    public Optional<Path> chooseMedia(Component owner,PptObject.Kind kind){
        DeFilter filter=switch(kind){
            case IMAGE->DeFilter.of("Imagens","png","jpg","jpeg");
            case AUDIO->DeFilter.of("Áudio","wav","mp3");
            case VIDEO->DeFilter.of("Vídeo","avi","mp4");
            default->throw new IllegalArgumentException("Objeto sem mídia: "+kind);
        };
        return Optional.ofNullable(OsFilePicker.openFile("Inserir mídia",filter)).map(File::toPath);
    }
}
