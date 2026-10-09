package dtm.stools.component.panels.editor.powerpoint.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PptSlide(String id, String title, Color background, List<PptObject> objects,
                       List<PptAnimation> animations, String transition) {
    public PptSlide {
        Objects.requireNonNull(id); Objects.requireNonNull(title); Objects.requireNonNull(background);
        objects = List.copyOf(objects); animations = List.copyOf(animations);
        transition = Objects.requireNonNullElse(transition, "cut");
    }
    public static PptSlide create(String title) {
        return new PptSlide(UUID.randomUUID().toString(), title, Color.WHITE, List.of(), List.of(), "cut");
    }
    public PptSlide duplicate() {
        return new PptSlide(UUID.randomUUID().toString(), title + " copy", background,
                objects.stream().map(PptObject::duplicate).toList(), List.of(), transition);
    }
    public PptSlide withTitle(String value) { return new PptSlide(id, value, background, objects, animations, transition); }
    public PptSlide withBackground(Color value) { return new PptSlide(id, title, value, objects, animations, transition); }
    public PptSlide withTransition(String value) { return new PptSlide(id, title, background, objects, animations, value); }
    public PptSlide withAnimations(List<PptAnimation> value) { return new PptSlide(id, title, background, objects, value, transition); }
    public PptSlide withObjects(List<PptObject> value) { return new PptSlide(id, title, background, value, animations, transition); }
    public PptSlide addObject(PptObject value) {
        List<PptObject> copy = new ArrayList<>(objects); copy.add(value); return withObjects(copy);
    }
    public PptSlide replaceObject(String objectId, PptObject value) {
        List<PptObject> copy = new ArrayList<>(objects);
        for (int i = 0; i < copy.size(); i++) if (copy.get(i).id().equals(objectId)) {
            PptObject previous=copy.set(i,value);boolean moved=previous.x()!=value.x()||previous.y()!=value.y()||previous.width()!=value.width()||previous.height()!=value.height()||previous.rotation()!=value.rotation();return withObjects(moved?updateConnections(copy,objectId):copy);
        }
        throw new IllegalArgumentException("Unknown object: " + objectId);
    }
    public PptSlide removeObject(String objectId) {
        List<PptObject> copy = new ArrayList<>(objects);
        if (!copy.removeIf(object -> object.id().equals(objectId))) return this;
        return new PptSlide(id, title, background, detachConnections(copy,objectId),
                animations.stream().filter(a -> !a.targetId().equals(objectId)).toList(), transition);
    }
    private static List<PptObject> detachConnections(List<PptObject> objects,String id){
        return objects.stream().map(o->{if(o.visual()==null||o.visual().connector()==null)return o;var c=o.visual().connector();return o.withVisual(o.visual().withConnector(new PptVisual.Connector(c.x1(),c.y1(),c.x2(),c.y2(),id.equals(c.startId())?null:c.startId(),id.equals(c.endId())?null:c.endId(),c.startSite(),c.endSite())));}).toList();
    }
    private static List<PptObject> updateConnections(List<PptObject> objects,String movedId){
        java.util.Map<String,PptObject> byId=new java.util.HashMap<>();for(PptObject o:objects)byId.put(o.id(),o);
        return objects.stream().map(o->{if(o.visual()==null||o.visual().connector()==null)return o;var c=o.visual().connector();
            if(!movedId.equals(c.startId())&&!movedId.equals(c.endId()))return o;
            double[] start={o.x()+c.x1(),o.y()+c.y1()},end={o.x()+c.x2(),o.y()+c.y2()};
            if(movedId.equals(c.startId())&&byId.containsKey(c.startId()))start=site(byId.get(c.startId()),c.startSite());if(movedId.equals(c.endId())&&byId.containsKey(c.endId()))end=site(byId.get(c.endId()),c.endSite());
            double x=Math.min(start[0],end[0]),y=Math.min(start[1],end[1]);
            return o.geometry(x,y,Math.max(1,Math.abs(end[0]-start[0])),Math.max(1,Math.abs(end[1]-start[1]))).withVisual(o.visual().withConnector(new PptVisual.Connector(start[0]-x,start[1]-y,end[0]-x,end[1]-y,c.startId(),c.endId(),c.startSite(),c.endSite())));
        }).toList();
    }
    private static double[] site(PptObject o,int index){double x=o.x()+o.width()/2,y=o.y()+o.height()/2;switch(index%4){case 0->y=o.y();case 1->x=o.x();case 2->y=o.y()+o.height();case 3->x=o.x()+o.width();}
        java.awt.geom.Point2D point=java.awt.geom.AffineTransform.getRotateInstance(Math.toRadians(o.rotation()),o.x()+o.width()/2,o.y()+o.height()/2).transform(new java.awt.geom.Point2D.Double(x,y),null);return new double[]{point.getX(),point.getY()};}
}
