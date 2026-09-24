package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class OpcPackageTest {
    private byte[] zip(String name,byte[] data)throws Exception{var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){zip.putNextEntry(new ZipEntry(name));zip.write(data);zip.closeEntry();}return out.toByteArray();}
    @Test void rejectsTraversalAndAbsolutePaths()throws Exception{
        for(String name:List.of("../outside","/absolute","a/../../b","a\\b","a//b")){byte[] bytes=zip(name,new byte[1]);assertThrows(IOException.class,()->OpcPackage.read(bytes,OpcPackage.Limits.DEFAULT),name);}
    }
    @Test void enforcesExpandedSizeBeforeAllocatingWholeArchive()throws Exception{
        byte[] bytes=zip("large.xml",new byte[100000]);assertThrows(IOException.class,()->OpcPackage.read(bytes,new OpcPackage.Limits(10000,1000,1000,10)));
    }
    @Test void copiesPartBytesOnEntryAndExit()throws Exception{
        byte[] data={1,2};OpcPackage p=new OpcPackage(Map.of("data",data));data[0]=9;p.part("data")[0]=8;assertEquals(1,p.part("data")[0]);
    }
}
