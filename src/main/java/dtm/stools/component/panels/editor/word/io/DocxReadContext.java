package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import dtm.stools.component.panels.editor.word.model.*;
import org.w3c.dom.Element;
import java.io.IOException;
import java.util.*;

final class DocxReadContext {
    final OpcPackage source;
    final String part;
    final DocxRelationships relationships;
    final DocxReadState state;

    DocxReadContext(OpcPackage source, String part, DocxReadState state) throws IOException {
        this.source = source; this.part = part; this.state = state;
        this.relationships = DocxRelationships.read(source,part);
    }
    String resource(String relationshipId) throws IOException {
        DocxRelationships.Relationship r = relationships.get(relationshipId).orElseThrow(() -> new IOException("Missing relationship " + relationshipId));
        if (r.external()) throw new IOException("External images are not embedded");
        WordResource resource = WordResource.of(source.part(r.target()),WordResource.contentTypeFor(r.target()));
        state.resources = state.resources.with(resource);
        return resource.id();
    }
    Element partXml(String relationshipId) throws IOException {
        DocxRelationships.Relationship r = relationships.get(relationshipId).orElseThrow(() -> new IOException("Missing relationship " + relationshipId));
        if (r.external()) throw new IOException("External part");
        return OoxmlXml.parse(source.part(r.target())).getDocumentElement();
    }
    Optional<DocxRelationships.Relationship> relationship(String id) { return relationships.get(id); }
    void note(String message) { state.diagnostics.add(message); }
}
