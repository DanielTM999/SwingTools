package dtm.stools.component.panels.editor.word.layout;

import dtm.stools.component.panels.editor.word.layout.WordLayout.*;
import dtm.stools.component.panels.editor.word.model.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CancellationException;

public class WordLayoutEngine {
    public static final FontRenderContext FONT_CONTEXT=new FontRenderContext(new AffineTransform(),true,true);
    public static final float DEFAULT_TAB=36;
    private static final Color INSERT_COLOR=new Color(0x1F7A3A),DELETE_COLOR=new Color(0xC0392B),COMMENT_BACKGROUND=new Color(0xFFF1B8);

    public WordLayout layout(WordDocument document) { return layout(document,false,document.pageSettings().contentWidth()); }
    public WordLayout layout(WordDocument document,boolean continuous,float continuousWidth) {
        Info info=Info.prepare(document);
        LayoutRun first=new LayoutRun(document,continuous,continuousWidth,info);
        WordLayout result=first.run();
        if(info.needsSecondPass) return new LayoutRun(document,continuous,continuousWidth,first.collected()).run();
        return result;
    }

    protected AttributedString attributed(WordParagraph paragraph) {
        return attributed(paragraph,0,paragraph.length(),Map.of(),Map.of(),WordTextStyle.DEFAULT);
    }

    protected AttributedString attributed(WordParagraph paragraph,int from,int to,Map<Integer,java.awt.font.GraphicAttribute> graphics,Map<Integer,WordLayoutGraphics.Tab> tabs,WordTextStyle emptyStyle) {
        String text=paragraph.text();
        if(text.isEmpty()||from>=to) {
            AttributedString empty=new AttributedString(" ");
            empty.addAttribute(TextAttribute.FONT,emptyStyle.font());
            return empty;
        }
        AttributedString result=new AttributedString(text.substring(from,to));int offset=0;
        for(WordInline inline:paragraph.runs()) {
            int runStart=offset,runEnd=offset+inline.length();offset=runEnd;
            int s=Math.max(from,runStart),e=Math.min(to,runEnd);
            if(s>=e) continue;
            int a=s-from,b=e-from;WordTextStyle style=inline.style();WordRevision revision=style.revision();
            Font font=style.verticalAlign()==WordTextStyle.VerticalAlign.BASELINE?style.font():style.font().deriveFont(Map.of(TextAttribute.SUPERSCRIPT,style.verticalAlign()==WordTextStyle.VerticalAlign.SUPERSCRIPT?TextAttribute.SUPERSCRIPT_SUPER:TextAttribute.SUPERSCRIPT_SUB));
            result.addAttribute(TextAttribute.FONT,font,a,b);
            result.addAttribute(TextAttribute.FOREGROUND,revision==null?style.foreground():revision.type()==WordRevision.Type.INSERT?INSERT_COLOR:DELETE_COLOR,a,b);
            if(style.underline()||revision!=null&&revision.type()==WordRevision.Type.INSERT) result.addAttribute(TextAttribute.UNDERLINE,TextAttribute.UNDERLINE_ON,a,b);
            if(style.strike()||revision!=null&&revision.type()==WordRevision.Type.DELETE) result.addAttribute(TextAttribute.STRIKETHROUGH,TextAttribute.STRIKETHROUGH_ON,a,b);
            if(style.highlight()!=null) result.addAttribute(TextAttribute.BACKGROUND,new Color(style.highlight()),a,b);
            else if(!style.comments().isEmpty()) result.addAttribute(TextAttribute.BACKGROUND,COMMENT_BACKGROUND,a,b);
            if(inline instanceof WordObjectRun) {
                var graphic=graphics.get(runStart);
                result.addAttribute(TextAttribute.CHAR_REPLACEMENT,graphic==null?new WordLayoutGraphics.Space(style.size()*0.5f,style.size()*0.8f,0):graphic,a,b);
            } else {
                for(int k=s;k<e;k++) if(text.charAt(k)=='\t') {
                    WordLayoutGraphics.Tab tab=tabs.get(k);
                    result.addAttribute(TextAttribute.CHAR_REPLACEMENT,tab==null?new WordLayoutGraphics.Tab(DEFAULT_TAB/2,style.size()*0.8f,style.size()*0.2f,null,null):tab,k-from,k-from+1);
                }
            }
        }
        return result;
    }

    private static void checkCancelled(){if(Thread.currentThread().isInterrupted())throw new CancellationException();}

    static final class Info {
        int totalPages;
        boolean needsSecondPass;
        final Map<UUID,Integer> headingPages=new HashMap<>();
        final Map<String,Integer> bookmarkPages=new HashMap<>();
        final Map<String,String> bookmarkTexts=new HashMap<>();
        final Map<String,Integer> noteNumbers=new HashMap<>();
        final List<String> endnotes=new ArrayList<>();
        static Info prepare(WordDocument document) {
            Info info=new Info();int footnote=0,endnote=0;
            for(WordObjectRef ref:document.objects()) {
                if(ref.object() instanceof WordNoteReference note&&!info.noteNumbers.containsKey(note.noteId())) {
                    WordNote n=document.parts().notes().get(note.noteId());
                    if(n!=null&&n.kind()==WordNote.Kind.ENDNOTE){info.noteNumbers.put(note.noteId(),++endnote);info.endnotes.add(note.noteId());}
                    else info.noteNumbers.put(note.noteId(),++footnote);
                }
                if(ref.object() instanceof WordField field&&(field.kind()==WordField.Kind.NUM_PAGES||field.kind()==WordField.Kind.PAGE_REF||field.kind()==WordField.Kind.REF)) info.needsSecondPass=true;
            }
            for(WordBlock block:document.blocks()) if(block instanceof WordTableOfContents) info.needsSecondPass=true;
            return info;
        }
    }

    static final class BoxFlow implements WordLayoutFlow {
        final List<Line> lines=new ArrayList<>();
        final List<ObjectBox> objects=new ArrayList<>();
        final List<Decoration> decorations=new ArrayList<>();
        final List<CellBox> cells=new ArrayList<>();
        final List<String> notes=new ArrayList<>();
        final float width;final Region region;final int pageNumber;float y;
        BoxFlow(float width,Region region,int pageNumber){this.width=width;this.region=region;this.pageNumber=pageNumber;}
        public float y(){return y;}
        public void setY(float value){y=value;}
        public float left(){return 0;}
        public float width(){return width;}
        public float top(){return 0;}
        public boolean paginated(){return false;}
        public boolean atTop(){return y<=0.5f;}
        public float remaining(){return Float.MAX_VALUE;}
        public void breakPage(boolean columnOnly){}
        public float[] slot(float top,float height,float x,float w){return new float[]{x,w,Float.NaN};}
        public void addLine(Line line){lines.add(line);}
        public void addObject(ObjectBox box){objects.add(box);}
        public void addDecoration(Decoration decoration){decorations.add(decoration);}
        public void addCell(CellBox cell){cells.add(cell);}
        public void addFloat(Rectangle2D.Float bounds,WordPlacement.Wrap wrap){}
        public void noteReferenced(String noteId){notes.add(noteId);}
        public int pageNumber(){return pageNumber;}
        public Region region(){return region;}
        float height(){return y;}
        void emit(WordLayoutFlow target,float dx,float dy,boolean repeated) {
            for(Decoration d:decorations) target.addDecoration(d.translate(dx,dy));
            for(Line l:lines) target.addLine(repeated?l.translate(dx,dy).as(l.region(),true):l.translate(dx,dy));
            for(ObjectBox o:objects) target.addObject(repeated?o.translate(dx,dy).as(o.region(),true):o.translate(dx,dy));
            for(CellBox c:cells) target.addCell(repeated?c.translate(dx,dy).repeat():c.translate(dx,dy));
            if(!repeated) for(String n:notes) target.noteReferenced(n);
        }
        BoxFlow slice(float from,float to) {
            BoxFlow part=new BoxFlow(width,region,pageNumber);
            for(Line l:lines) if(l.top()>=from-0.01f&&l.top()<to) part.lines.add(l.translate(0,-from));
            for(ObjectBox o:objects) if(o.y()>=from-0.01f&&o.y()<to) part.objects.add(o.translate(0,-from));
            for(Decoration d:decorations){var b=d.shape().getBounds2D();if(b.getY()>=from-0.01f&&b.getY()<to)part.decorations.add(d.translate(0,-from));}
            for(CellBox c:cells) if(c.bounds().y>=from-0.01f&&c.bounds().y<to) part.cells.add(c.translate(0,-from));
            for(String n:notes) part.notes.add(n);
            part.y=Math.min(to,y)-from;
            return part;
        }
        float cutBefore(float limit) {
            float cut=0;
            for(Line l:lines) if(l.bottom()<=limit) cut=Math.max(cut,l.bottom());
            boolean changed=true;int guard=0;
            while(changed&&guard++<200) {
                changed=false;
                for(Line l:lines) if(l.top()<cut-0.01f&&l.bottom()>cut+0.01f){cut=l.top();changed=true;}
                for(ObjectBox o:objects) if(o.y()<cut-0.01f&&o.y()+o.height()>cut+0.01f){cut=o.y();changed=true;}
            }
            return Math.max(0,cut);
        }
    }

    private final class LayoutRun {
        final WordDocument doc;final boolean continuous;final float continuousWidth;final Info info;
        final List<PageBuilder> pages=new ArrayList<>();
        final Map<String,int[]> counters=new HashMap<>();final Set<String> restarted=new HashSet<>();
        final Map<String,Integer> sequences=new HashMap<>();
        final Info collected=new Info();
        final PageFlow body=new PageFlow();
        PageBuilder page;WordPageSettings settings;int nextNumber=1;int knownTotal;

        LayoutRun(WordDocument doc,boolean continuous,float continuousWidth,Info info){this.doc=doc;this.continuous=continuous;this.continuousWidth=continuousWidth;this.info=info;collected.noteNumbers.putAll(info.noteNumbers);collected.endnotes.addAll(info.endnotes);}
        Info collected(){collected.totalPages=pages.size();return collected;}

        WordLayout run() {
            List<WordBlock> blocks=doc.blocks();
            int[] section=new int[blocks.size()];WordPageSettings[] settingsOf=new WordPageSettings[blocks.size()];
            WordPageSettings current=doc.pageSettings();int index=0;
            for(int i=blocks.size()-1;i>=0;i--){if(blocks.get(i) instanceof WordParagraph p&&p.sectionBreak()!=null){current=p.sectionBreak();index++;}settingsOf[i]=current;section[i]=index;}
            settings=settingsOf.length>0?settingsOf[0]:doc.pageSettings();
            if(settings.pageNumberStart()>0) nextNumber=settings.pageNumberStart();
            newPage(true);
            int[] offset={0};int currentSection=section.length>0?section[0]:0;
            for(int i=0;i<blocks.size();i++) {
                checkCancelled();
                if(section[i]!=currentSection) {
                    currentSection=section[i];settings=settingsOf[i];
                    if(!continuous){finishPage();if(settings.pageNumberStart()>0)nextNumber=settings.pageNumberStart();newPage(true);}
                }
                if(!continuous&&blocks.get(i) instanceof WordParagraph p&&p.style().keepWithNext()&&!body.atTop()&&i+1<blocks.size()){
                    BoxFlow probe=measure(List.of(p),body.width(),Region.BODY,body.pageNumber());
                    float next=blocks.get(i+1) instanceof WordParagraph n?Math.max(14,n.style().before()+16*n.style().lineSpacing()):32;
                    if(!body.fits(probe.height()+next))body.breakPage(false);
                }
                block(blocks.get(i),offset,body,true,0);
            }
            endnotes();
            finishPage();
            knownTotal=pages.size();
            if(!continuous) for(PageBuilder p:pages) headerFooter(p);
            List<Page> result=new ArrayList<>();
            for(PageBuilder p:pages) {
                float width=continuous?continuousWidth+p.settings.left()+p.settings.right():p.settings.width();
                float height=continuous?Math.max(p.settings.height(),p.y+p.settings.bottom()):p.settings.height();
                List<ObjectBox> objects=new ArrayList<>(p.objects);
                objects.sort(Comparator.comparingInt((ObjectBox o)->o.floating()?o.object().placement().zOrder()+(o.behindText()?-100000:0):0));
                result.add(new Page(p.index,width,height,p.lines,p.decorations,objects,p.cells,p.number,p.settings,doc));
            }
            return new WordLayout(doc,result,collected.bookmarkPages);
        }

        void newPage(boolean sectionStart) {
            page=new PageBuilder(pages.size(),nextNumber++,settings,sectionStart);
            pages.add(page);
            page.bodyTop=settings.top();page.bodyBottom=continuous?Float.MAX_VALUE:settings.height()-settings.bottom();
            if(!continuous) {
                List<WordBlock> header=doc.parts().headers().header(page.number,sectionStart),footer=doc.parts().headers().footer(page.number,sectionStart);
                if(!header.isEmpty()) page.bodyTop=Math.max(settings.top(),settings.headerDistance()+measure(header,settings.contentWidth(),Region.HEADER,page.number).height()+6);
                if(!footer.isEmpty()) page.bodyBottom=Math.min(page.bodyBottom,settings.height()-settings.footerDistance()-measure(footer,settings.contentWidth(),Region.FOOTER,page.number).height()-6);
                if(page.bodyBottom-page.bodyTop<72) page.bodyBottom=page.bodyTop+72;
            }
            page.y=page.bodyTop;
        }
        void finishPage() {
            if(page==null) return;
            if(!page.notes.isEmpty()) {
                WordPageSettings s=page.settings;float y=page.bodyBottom-page.noteReserve;float left=s.left();
                page.decorations.add(new Decoration(Decoration.Kind.STROKE,new Line2D.Float(left,y+5,left+Math.min(144,s.contentWidth()/3),y+5),0x808080,0.6f,WordBorder.Style.SINGLE));
                y+=10;
                for(String id:page.notes){BoxFlow note=noteFlow(id,s.contentWidth(),page.number);note.emit(new Collector(page,Region.NOTE),left,y,false);y+=note.height();}
            }
        }
        void headerFooter(PageBuilder p) {
            List<WordBlock> header=doc.parts().headers().header(p.number,p.sectionStart),footer=doc.parts().headers().footer(p.number,p.sectionStart);
            if(!header.isEmpty()){BoxFlow h=measure(header,p.settings.contentWidth(),Region.HEADER,p.number);h.emit(new Collector(p,Region.HEADER),p.settings.left(),p.settings.headerDistance(),false);}
            if(!footer.isEmpty()){BoxFlow f=measure(footer,p.settings.contentWidth(),Region.FOOTER,p.number);f.emit(new Collector(p,Region.FOOTER),p.settings.left(),p.settings.height()-p.settings.footerDistance()-f.height(),false);}
        }
        BoxFlow measure(List<WordBlock> blocks,float width,Region region,int number) {
            BoxFlow flow=new BoxFlow(width,region,number);int[] offset={0};
            for(WordBlock b:blocks) block(b,offset,flow,false,0);
            return flow;
        }
        BoxFlow noteFlow(String id,float width,int number) {
            WordNote note=doc.parts().notes().get(id);BoxFlow flow=new BoxFlow(width,Region.NOTE,number);
            if(note==null) return flow;
            Integer n=info.noteNumbers.getOrDefault(id,collected.noteNumbers.get(id));
            boolean first=true;int[] offset={0};
            for(WordParagraph p:note.paragraphs()) {
                WordParagraph shown=p;
                if(first&&n!=null){List<WordInline> runs=new ArrayList<>();WordTextStyle s=p.runs().isEmpty()?doc.styles().resolveText("FootnoteText"):p.runs().getFirst().style();
                    runs.add(new WordRun(n+" ",s.withVerticalAlign(WordTextStyle.VerticalAlign.SUPERSCRIPT)));runs.addAll(p.runs());shown=p.withRuns(runs);}
                first=false;block(shown,offset,flow,false,0);
            }
            return flow;
        }
        void endnotes() {
            if(info.endnotes.isEmpty()) return;
            float left=body.left();body.setY(body.y()+12);
            body.addDecoration(new Decoration(Decoration.Kind.STROKE,new Line2D.Float(left,body.y(),left+Math.min(144,body.width()/3),body.y()),0x808080,0.6f,WordBorder.Style.SINGLE));
            body.setY(body.y()+6);
            for(String id:info.endnotes){BoxFlow note=noteFlow(id,body.width(),body.pageNumber());if(!body.fits(note.height())&&!body.atTop())body.breakPage(false);note.emit(new Collector(page,Region.NOTE),left,body.y(),false);body.setY(body.y()+note.height());}
        }

        void block(WordBlock block,int[] offset,WordLayoutFlow flow,boolean positional,int depth) {
            switch(block) {
                case WordParagraph p -> {paragraph(p,positional?offset[0]:-1,flow,positional,null);offset[0]+=p.length()+1;}
                case WordTable t -> table(t,offset,flow,positional,depth);
                case WordTableOfContents toc -> toc(toc,flow);
                default -> placeholder(block,flow);
            }
        }

        void paragraph(WordParagraph p,int start,WordLayoutFlow flow,boolean positional,String link) {
            WordParagraphStyle style=p.style();
            if(style.pageBreakBefore()&&flow.paginated()&&!flow.atTop()) flow.breakPage(false);
            flow.setY(flow.y()+style.before());
            boolean body=positional&&flow.region()==Region.BODY;
            if(body){if(style.headingLevel()>0)collected.headingPages.put(p.id(),flow.pageNumber());for(String b:p.bookmarks())collected.bookmarkPages.put(b,flow.pageNumber());}
            String text=p.text();
            WordTextStyle emptyStyle=p.runs().isEmpty()?doc.styles().resolveText(style.styleId()):p.runs().getFirst().style();
            float baseSize=emptyStyle.size();
            float left=style.leftIndent(),right=style.rightIndent(),first=style.firstLineIndent();
            String markerText=null;
            if(style.list()!=null) {
                WordListDefinition definition=doc.parts().numbering().get(style.list().listId()).orElse(WordListDefinition.bullets(style.list().listId()));
                WordListLevel level=definition.level(style.list().level());
                if(left==0&&first==0){left=level.indent();first=-level.hanging();}
                markerText=marker(definition,style.list().level(),body);
            }
            float maxObjectWidth=Math.max(24,flow.width()-left-right);
            Map<Integer,java.awt.font.GraphicAttribute> graphics=new HashMap<>();Map<Integer,String> labels=new HashMap<>();
            prepareObjects(p,flow,maxObjectWidth,graphics,labels,body);
            float paragraphTop=flow.y();
            placeFloats(p,start,flow,paragraphTop,positional);
            AttributedString attributed=attributed(p,0,text.length(),graphics,Map.of(),emptyStyle);
            LineBreakMeasurer measurer=new LineBreakMeasurer(attributed.getIterator(),FONT_CONTEXT);
            List<Integer> breaks=new ArrayList<>();int cursor=0;
            for(WordInline inline:p.runs()){if(inline instanceof WordObjectRun o&&o.object() instanceof WordBreak)breaks.add(cursor);cursor+=inline.length();}
            boolean firstLine=true;int guard=0;float shadeTop=flow.y();
            while(measurer.getPosition()<Math.max(1,text.length())) {
                checkCancelled();
                int lineStart=measurer.getPosition();
                float indent=firstLine?(markerText!=null&&first<0?0:first):0;
                TextLayout markerLayout=null;
                if(firstLine&&markerText!=null&&!markerText.isEmpty()){markerLayout=new TextLayout(markerText,emptyStyle.withVerticalAlign(WordTextStyle.VerticalAlign.BASELINE).font(),FONT_CONTEXT);if(first>=0)indent=first+markerLayout.getAdvance()+4;}
                float x=flow.left()+left+indent,available=Math.max(12,flow.width()-left-right-indent);
                float[] slot=flow.slot(flow.y(),baseSize*1.3f*style.lineSpacing(),x,available);
                if(!Float.isNaN(slot[2])&&guard++<60){flow.setY(slot[2]);continue;}
                x=slot[0];available=Math.max(12,slot[1]);
                int limit=text.length();for(int b:breaks)if(b>=lineStart){limit=b+1;break;}
                TextLayout measured=text.isEmpty()?measurer.nextLayout(available):measurer.nextLayout(available,limit,false);
                if(measured==null) break;
                int lineEnd=measurer.getPosition();
                boolean endsWithBreak=lineEnd>0&&breaks.contains(lineEnd-1),lastLine=lineEnd>=text.length();
                TextLayout line=measured;
                if(text.substring(Math.min(lineStart,text.length()),Math.min(lineEnd,text.length())).indexOf('\t')>=0) line=tabbed(p,lineStart,lineEnd,flow,x,style,graphics,emptyStyle);
                if(style.alignment()==WordParagraphStyle.Alignment.JUSTIFY&&!lastLine&&!endsWithBreak&&line.getVisibleAdvance()<available&&line.getCharacterCount()>1)
                    try{line=line.getJustifiedLayout(available);}catch(IllegalStateException ignored){}
                boolean hasObject=false;
                for(int i=lineStart;i<Math.min(lineEnd,text.length());i++) if(text.charAt(i)==WordObjectRun.PLACEHOLDER){WordObjectRun o=p.objectAt(i);if(o!=null&&!o.object().textual()&&!o.object().placement().floating())hasObject=true;}
                float natural=line.getAscent()+line.getDescent()+line.getLeading();
                float height=hasObject?natural+(style.lineSpacing()-1)*baseSize*1.2f:natural*style.lineSpacing();
                if(flow.paginated()&&!flow.fits(height)&&!flow.atTop()&&guard++<400){flow.breakPage(false);measurer.setPosition(lineStart);shadeTop=flow.y();continue;}
                float visible=line.getVisibleAdvance();
                float shift=switch(style.alignment()){case RIGHT->Math.max(0,available-visible);case CENTER->Math.max(0,(available-visible)/2);default->0;};
                float baseline=flow.y()+line.getAscent();
                int absStart=start<0?-1:start+Math.min(lineStart,text.length()),absEnd=start<0?-1:start+Math.min(lineEnd,text.length());
                float markerX=flow.left()+left+first;
                flow.addLine(new Line(absStart,absEnd,x+shift,baseline,line,available,flow.region(),false,markerLayout,markerX,link));
                for(int i=lineStart;i<Math.min(lineEnd,text.length());i++) {
                    if(text.charAt(i)!=WordObjectRun.PLACEHOLDER) continue;
                    WordObjectRun run=p.objectAt(i);if(run==null) continue;
                    WordInlineObject object=run.object();
                    if(object.placement().floating()) continue;
                    var graphic=graphics.get(i);if(graphic==null) continue;
                    float ox=x+shift+line.getCaretInfo(TextHitInfo.leading(i-lineStart))[0];
                    flow.addObject(new ObjectBox(start<0?-1:start+i,object,ox,baseline-graphic.getAscent(),graphic.getAdvance(),graphic.getAscent()+graphic.getDescent(),false,flow.region(),false));
                    if(object instanceof WordNoteReference note) flow.noteReferenced(note.noteId());
                }
                flow.setY(flow.y()+height);
                firstLine=false;
                if(endsWithBreak&&flow.paginated()){WordObjectRun br=p.objectAt(lineEnd-1);if(br!=null&&br.object() instanceof WordBreak b&&b.kind()!=WordBreak.Kind.LINE){flow.breakPage(b.kind()==WordBreak.Kind.COLUMN);shadeTop=flow.y();}}
                if(text.isEmpty()) break;
            }
            if(style.shading()!=null) flow.addDecoration(new Decoration(Decoration.Kind.FILL,new Rectangle2D.Float(flow.left()+left-2,shadeTop,flow.width()-left-right+4,flow.y()-shadeTop),style.shading(),0,WordBorder.Style.SINGLE));
            flow.setY(flow.y()+style.after());
            if(body&&!p.bookmarks().isEmpty()) {
                StringBuilder shown=new StringBuilder();int k=0;
                for(char c:text.toCharArray()){shown.append(c==WordObjectRun.PLACEHOLDER?labels.getOrDefault(k,""):String.valueOf(c));k++;}
                for(String b:p.bookmarks()) collected.bookmarkTexts.put(b,shown.toString());
            }
        }

        void prepareObjects(WordParagraph p,WordLayoutFlow flow,float maxWidth,Map<Integer,java.awt.font.GraphicAttribute> graphics,Map<Integer,String> labels,boolean body) {
            int offset=0;
            for(WordInline inline:p.runs()) {
                if(inline instanceof WordObjectRun run) {
                    WordInlineObject object=run.object();WordTextStyle style=run.style();Font font=style.font();
                    switch(object) {
                        case WordBreak b -> graphics.put(offset,new WordLayoutGraphics.Space(0.01f,style.size()*0.8f,style.size()*0.2f));
                        case WordField f -> {String t=fieldText(f,flow,body);labels.put(offset,t);graphics.put(offset,new WordLayoutGraphics.Label(t,font,style.foreground(),null,false));}
                        case WordNoteReference n -> {Integer number=info.noteNumbers.get(n.noteId());String t=number==null?"*":number.toString();labels.put(offset,t);graphics.put(offset,new WordLayoutGraphics.Label(t,font,style.foreground(),null,true));}
                        case WordFormField f -> {labels.put(offset,f.plainText());graphics.put(offset,new WordLayoutGraphics.Label(f.display(),font,f.value().isEmpty()&&f.kind()!=WordFormField.Kind.CHECKBOX?new Color(0x6B7280):style.foreground(),new Color(0xE5E7EB),false));}
                        default -> {
                            if(object.placement().floating()) graphics.put(offset,new WordLayoutGraphics.Space(0.01f,style.size()*0.8f,style.size()*0.2f));
                            else {
                                float w=Math.max(1,object.width()),h=Math.max(1,object.height());
                                if(w>maxWidth){h=h*maxWidth/w;w=maxWidth;}
                                float ascent=object instanceof WordEquation eq?Math.min(h,eq.ascent()*(h/Math.max(1,object.height()))):h;
                                graphics.put(offset,new WordLayoutGraphics.Space(w,ascent,h-ascent));
                            }
                        }
                    }
                }
                offset+=inline.length();
            }
        }

        void placeFloats(WordParagraph p,int start,WordLayoutFlow flow,float paragraphTop,boolean positional) {
            int offset=0;
            for(WordInline inline:p.runs()) {
                if(inline instanceof WordObjectRun run&&run.object().placement().floating()&&!run.object().textual()) {
                    WordInlineObject object=run.object();WordPlacement placement=object.placement();
                    boolean page=flow.paginated();
                    float x=placement.horizontalFrom()==WordPlacement.Anchor.PAGE&&page?placement.x():flow.left()+placement.x();
                    float y=switch(placement.verticalFrom()){case PAGE->page?placement.y():paragraphTop+placement.y();case MARGIN->page?flow.top()+placement.y():paragraphTop+placement.y();default->paragraphTop+placement.y();};
                    Rectangle2D.Float bounds=new Rectangle2D.Float(x,y,object.width(),object.height());
                    flow.addObject(new ObjectBox(start<0?-1:start+offset,object,x,y,object.width(),object.height(),true,flow.region(),false));
                    if(placement.wrapsText()) flow.addFloat(bounds,placement.wrap());
                }
                offset+=inline.length();
            }
        }

        TextLayout tabbed(WordParagraph p,int from,int to,WordLayoutFlow flow,float lineX,WordParagraphStyle style,Map<Integer,java.awt.font.GraphicAttribute> graphics,WordTextStyle emptyStyle) {
            String text=p.text();Map<Integer,WordLayoutGraphics.Tab> tabs=new HashMap<>();
            List<Integer> positions=new ArrayList<>();for(int i=from;i<Math.min(to,text.length());i++)if(text.charAt(i)=='\t')positions.add(i);
            for(int n=0;n<positions.size();n++) {
                int t=positions.get(n);
                TextLayout current=new TextLayout(attributed(p,from,to,graphics,tabs,emptyStyle).getIterator(),FONT_CONTEXT);
                float x=lineX+current.getCaretInfo(TextHitInfo.leading(t-from))[0]-flow.left();
                int next=n+1<positions.size()?positions.get(n+1):Math.min(to,text.length());
                float segment=current.getCaretInfo(TextHitInfo.leading(next-from))[0]-current.getCaretInfo(TextHitInfo.leading(t+1-from))[0];
                WordTabStop stop=null;
                for(WordTabStop s:style.tabs()) if(s.position()>x+0.5f){stop=s;break;}
                if(stop==null) stop=WordTabStop.left((float)(Math.floor(x/DEFAULT_TAB)+1)*DEFAULT_TAB);
                float width=switch(stop.alignment()){case LEFT->stop.position()-x;case CENTER->stop.position()-x-segment/2;default->stop.position()-x-segment;};
                WordTextStyle s=p.styleAt(t);
                tabs.put(t,new WordLayoutGraphics.Tab(Math.max(1,width),s.size()*0.8f,s.size()*0.2f,stop.leader(),s.foreground()));
            }
            return new TextLayout(attributed(p,from,to,graphics,tabs,emptyStyle).getIterator(),FONT_CONTEXT);
        }

        String marker(WordListDefinition definition,int level,boolean count) {
            WordListLevel l=definition.level(level);
            int[] c=counters.computeIfAbsent(definition.abstractId(),k->new int[9]);
            if(count) {
                if(definition.restart()&&restarted.add(definition.id())) Arrays.fill(c,0);
                c[level]=c[level]==0?l.start():c[level]+1;
                for(int k=level+1;k<9;k++) c[k]=0;
            }
            if(l.format()==WordListLevel.Format.BULLET) return l.text();
            if(l.format()==WordListLevel.Format.NONE) return "";
            String result=l.text();
            for(int k=0;k<=level&&k<9;k++){WordListLevel lk=definition.level(k);result=result.replace("%"+(k+1),WordListLevel.number(lk.format(),c[k]==0?lk.start():c[k]));}
            return result;
        }

        String fieldText(WordField field,WordLayoutFlow flow,boolean body) {
            return switch(field.kind()) {
                case PAGE -> Integer.toString(flow.pageNumber());
                case NUM_PAGES -> Integer.toString(knownTotal>0?knownTotal:info.totalPages>0?info.totalPages:Math.max(1,pages.size()));
                case DATE -> {
                    try{yield new SimpleDateFormat(field.argument().isBlank()?"dd/MM/yyyy":field.argument().replace("'",""),Locale.forLanguageTag("pt-BR")).format(new Date());}
                    catch(IllegalArgumentException invalid){yield new SimpleDateFormat("dd/MM/yyyy").format(new Date());}
                }
                case REF -> info.bookmarkTexts.getOrDefault(field.argument(),field.cachedText().isEmpty()?"Erro! Indicador não definido.":field.cachedText());
                case PAGE_REF -> {Integer n=info.bookmarkPages.get(field.argument());yield n==null?field.cachedText().isEmpty()?"?":field.cachedText():n.toString();}
                case SEQ -> {if(!body)yield "1";int n=sequences.merge(field.argument(),1,Integer::sum);yield Integer.toString(n);}
            };
        }

        void table(WordTable table,int[] offset,WordLayoutFlow flow,boolean positional,int depth) {
            float available=flow.width(),natural=table.width(),scale=natural>available?available/natural:1;
            float width=natural*scale;
            float x0=flow.left()+switch(table.alignment()){case CENTER->(available-width)/2;case RIGHT->available-width;default->0;};
            int columns=table.gridColumns();float[] edges=new float[columns+1];
            for(int i=0;i<columns;i++) edges[i+1]=edges[i]+table.columnWidths().get(i)*scale;
            float pad=Math.min(table.cellPadding(),4+table.cellPadding()*scale);
            int rowCount=table.rows().size();BoxFlow[][] content=new BoxFlow[rowCount][];float[] heights=new float[rowCount];
            for(int r=0;r<rowCount;r++) {
                WordTableRow row=table.rows().get(r);content[r]=new BoxFlow[row.cells().size()];heights[r]=row.height();
                for(int c=0;c<row.cells().size();c++) {
                    WordTableCell cell=row.cells().get(c);int col=Math.min(columns-1,row.columnOf(c));
                    float cellWidth=Math.max(8,edges[Math.min(columns,col+cell.gridSpan())]-edges[col]-2*pad);
                    BoxFlow box=new BoxFlow(cellWidth,flow.region(),flow.pageNumber());
                    for(WordBlock b:cell.blocks()) block(b,offset,box,positional,depth+1);
                    content[r][c]=box;
                    if(cell.verticalMerge()==WordTableCell.Merge.NONE) heights[r]=Math.max(heights[r],box.height()+2*pad);
                    else heights[r]=Math.max(heights[r],cell.verticalMerge()==WordTableCell.Merge.CONTINUE?2*pad+4:heights[r]);
                }
            }
            for(int r=0;r<rowCount;r++) {
                WordTableRow row=table.rows().get(r);
                for(int c=0;c<row.cells().size();c++) {
                    if(row.cells().get(c).verticalMerge()!=WordTableCell.Merge.RESTART) continue;
                    int span=mergeSpan(table,r,row.columnOf(c));float sum=0;for(int k=r;k<r+span;k++)sum+=heights[k];
                    float needed=content[r][c].height()+2*pad;
                    if(needed>sum) heights[r+span-1]+=needed-sum;
                }
            }
            int headerRows=0;while(headerRows<rowCount&&table.rows().get(headerRows).header())headerRows++;
            float[] absolute=new float[edges.length];for(int i=0;i<edges.length;i++)absolute[i]=x0+edges[i];
            for(int r=0;r<rowCount;r++) {
                checkCancelled();
                float h=heights[r];
                if(flow.paginated()&&!flow.fits(h)) {
                    boolean split=!table.rows().get(r).cantSplit()&&flow.remaining()>=Math.min(48,h/2)&&h>flow.remaining();
                    boolean tooTall=h>(flow.remaining()+(flow.atTop()?0:Float.MAX_VALUE/4));
                    if(split||tooTall){r=splitRow(table,r,content,heights,flow,x0,edges,absolute,pad,headerRows,depth);continue;}
                    if(!flow.atTop()){flow.breakPage(false);if(r>=headerRows)for(int hr=0;hr<headerRows;hr++)placeRow(table,hr,content,heights,flow,x0,edges,absolute,pad,true,depth,heights[hr],null);}
                }
                placeRow(table,r,content,heights,flow,x0,edges,absolute,pad,false,depth,h,null);
            }
        }
        int mergeSpan(WordTable table,int row,int column) {
            int span=1;
            while(row+span<table.rows().size()){WordTableCell next=table.cell(row+span,column);if(next==null||next.verticalMerge()!=WordTableCell.Merge.CONTINUE)break;span++;}
            return span;
        }
        void placeRow(WordTable table,int r,BoxFlow[][] content,float[] heights,WordLayoutFlow flow,float x0,float[] edges,float[] absolute,float pad,boolean repeated,int depth,float height,BoxFlow[] override) {
            WordTableRow row=table.rows().get(r);float y=flow.y();int columns=table.gridColumns();
            for(int c=0;c<row.cells().size();c++) {
                WordTableCell cell=row.cells().get(c);int col=Math.min(columns-1,row.columnOf(c));
                float cx=x0+edges[col],cw=edges[Math.min(columns,col+cell.gridSpan())]-edges[col];
                float ch=height;
                if(cell.verticalMerge()==WordTableCell.Merge.RESTART&&override==null){int span=mergeSpan(table,r,col);ch=0;for(int k=r;k<r+span;k++)ch+=heights[k];}
                if(cell.verticalMerge()==WordTableCell.Merge.CONTINUE) continue;
                Rectangle2D.Float rect=new Rectangle2D.Float(cx,y,cw,ch);
                if(cell.fill()!=null) flow.addDecoration(new Decoration(Decoration.Kind.FILL,rect,cell.fill(),0,WordBorder.Style.SINGLE));
                WordBorder border=cell.border()!=null?cell.border():table.border();
                if(border.visible()) flow.addDecoration(new Decoration(Decoration.Kind.STROKE,rect,border.color(),border.width(),border.style()));
                BoxFlow box=override!=null?override[c]:content[r][c];
                if(box==null) continue;
                float shift=override!=null?0:switch(cell.verticalAlign()){case CENTER->Math.max(0,(ch-2*pad-box.height())/2);case BOTTOM->Math.max(0,ch-2*pad-box.height());default->0;};
                box.emit(flow,cx+pad,y+pad+shift,repeated);
                flow.addCell(new CellBox(table.id(),r,c,col,cell.gridSpan(),rect,repeated,depth,absolute));
            }
            flow.setY(y+height);
        }
        int splitRow(WordTable table,int r,BoxFlow[][] content,float[] heights,WordLayoutFlow flow,float x0,float[] edges,float[] absolute,float pad,int headerRows,int depth) {
            BoxFlow[] remaining=content[r].clone();
            int guard=0;
            while(guard++<500) {
                float space=flow.remaining()-2*pad;
                boolean fitsAll=true;float needed=0;
                for(BoxFlow b:remaining) if(b!=null){needed=Math.max(needed,b.height());if(b.height()>space)fitsAll=false;}
                if(fitsAll){placeRow(table,r,content,heights,flow,x0,edges,absolute,pad,false,depth,Math.max(needed+2*pad,table.rows().get(r).height()),remaining);return r;}
                BoxFlow[] now=new BoxFlow[remaining.length],later=new BoxFlow[remaining.length];float used=0;boolean progress=false;
                for(int c=0;c<remaining.length;c++) {
                    BoxFlow b=remaining[c];if(b==null)continue;
                    float cut=b.height()<=space?b.height():b.cutBefore(space);
                    if(cut<=0) cut=0; else progress=true;
                    now[c]=b.slice(0,cut);later[c]=b.slice(cut,Math.max(cut,b.height()));used=Math.max(used,cut);
                }
                if(!progress&&!flow.atTop()){flow.breakPage(false);for(int hr=0;hr<headerRows;hr++)placeRow(table,hr,content,heights,flow,x0,edges,absolute,pad,true,depth,heights[hr],null);continue;}
                if(!progress){placeRow(table,r,content,heights,flow,x0,edges,absolute,pad,false,depth,needed+2*pad,remaining);return r;}
                placeRow(table,r,content,heights,flow,x0,edges,absolute,pad,false,depth,used+2*pad,now);
                flow.breakPage(false);
                for(int hr=0;hr<headerRows;hr++)placeRow(table,hr,content,heights,flow,x0,edges,absolute,pad,true,depth,heights[hr],null);
                remaining=later;
                boolean empty=true;for(BoxFlow b:remaining)if(b!=null&&(!b.lines.isEmpty()||!b.objects.isEmpty()))empty=false;
                if(empty) return r;
            }
            return r;
        }

        void toc(WordTableOfContents toc,WordLayoutFlow flow) {
            WordStyleSheet styles=doc.styles();
            if(!toc.title().isBlank()) paragraph(new WordParagraph(UUID.randomUUID(),List.of(new WordRun(toc.title(),styles.resolveText("TOCHeading"))),styles.resolveParagraph("TOCHeading").withHeadingLevel(0)),-1,flow,false,null);
            boolean any=false;
            for(WordParagraph heading:doc.paragraphs()) {
                int level=heading.style().headingLevel();
                if(level<1||level>toc.maxLevel()||heading.plainText().isBlank()) continue;
                any=true;
                Integer page=info.headingPages.get(heading.id());
                String id="TOC"+Math.min(3,level);
                WordParagraphStyle ps=styles.resolveParagraph(id).withTabs(List.of(new WordTabStop(Math.max(36,flow.width()-2),WordTabStop.Alignment.RIGHT,WordTabStop.Leader.DOT)));
                WordTextStyle ts=styles.resolveText(id);
                String label=heading.plainText().replace('\t',' ').replace(" "," ");
                paragraph(new WordParagraph(UUID.randomUUID(),List.of(new WordRun(label+"\t"+(page==null?"?":page),ts)),ps),-1,flow,false,"#p:"+heading.id());
            }
            if(!any) paragraph(WordParagraph.of("Nenhum título encontrado. Aplique estilos de título ao documento.",styles.resolveText(null).withItalic(true).withColor(0x6B7280),WordParagraphStyle.DEFAULT),-1,flow,false,null);
        }

        void placeholder(WordBlock block,WordLayoutFlow flow) {
            String label=block instanceof WordOpaqueBlock o?o.label():block.label();
            String preview=block.plainText().replace('\n',' ').replace('\t',' ');
            if(preview.length()>400) preview=preview.substring(0,400)+"…";
            float top=flow.y()+2;flow.setY(top+4);
            WordTextStyle style=WordTextStyle.DEFAULT.withSize(9).withItalic(true).withColor(0x6B7280);
            paragraph(new WordParagraph(UUID.randomUUID(),List.of(new WordRun("Conteúdo preservado: "+label+(preview.isBlank()?"":" — "+preview),style)),WordParagraphStyle.DEFAULT.withSpacing(0,0,1).withIndents(6,6,0)),-1,flow,false,null);
            flow.addDecoration(new Decoration(Decoration.Kind.LABEL_BOX,new Rectangle2D.Float(flow.left(),top,flow.width(),flow.y()-top+4),0x9CA3AF,0.8f,WordBorder.Style.DASHED));
            flow.setY(flow.y()+10);
        }

        final class PageFlow implements WordLayoutFlow {
            public float y(){return page.y;}
            public void setY(float value){page.y=value;}
            public float left(){return continuous?page.settings.left():page.settings.left()+page.column*(page.settings.columnWidth()+page.settings.columnSpacing());}
            public float width(){return continuous?continuousWidth:page.settings.columnWidth();}
            public float top(){return page.settings.top();}
            public boolean paginated(){return !continuous;}
            public boolean atTop(){return page.y<=page.bodyTop+0.5f;}
            public float remaining(){return continuous?Float.MAX_VALUE:page.bodyBottom-page.noteReserve-page.y;}
            public boolean fits(float height){return continuous||page.y+height<=page.bodyBottom-page.noteReserve+0.01f;}
            public void breakPage(boolean columnOnly) {
                if(continuous) return;
                if(columnOnly&&page.column<page.settings.columns()-1){page.column++;page.y=page.bodyTop;return;}
                finishPage();newPage(false);
            }
            public float[] slot(float top,float height,float x,float width) {
                float lx=x,rx=x+width,push=Float.NaN;
                for(FloatBox f:page.floats) {
                    if(top+height<=f.bounds.y||top>=f.bounds.y+f.bounds.height) continue;
                    if(f.wrap==WordPlacement.Wrap.TOP_AND_BOTTOM){push=Float.isNaN(push)?f.bounds.y+f.bounds.height:Math.max(push,f.bounds.y+f.bounds.height);continue;}
                    if(f.bounds.x+f.bounds.width+6<=lx||f.bounds.x-6>=rx) continue;
                    float center=f.bounds.x+f.bounds.width/2;
                    if(center<=(lx+rx)/2) lx=Math.max(lx,f.bounds.x+f.bounds.width+6); else rx=Math.min(rx,f.bounds.x-6);
                }
                if(Float.isNaN(push)&&rx-lx<36) for(FloatBox f:page.floats) if(!(top+height<=f.bounds.y||top>=f.bounds.y+f.bounds.height)) push=Float.isNaN(push)?f.bounds.y+f.bounds.height:Math.max(push,f.bounds.y+f.bounds.height);
                if(!Float.isNaN(push)&&push<=top) push=top+1;
                return new float[]{lx,Math.max(12,rx-lx),push};
            }
            public void addLine(Line line){page.lines.add(line);}
            public void addObject(ObjectBox box){page.objects.add(box);}
            public void addDecoration(Decoration decoration){page.decorations.add(decoration);}
            public void addCell(CellBox cell){page.cells.add(cell);}
            public void addFloat(Rectangle2D.Float bounds,WordPlacement.Wrap wrap){page.floats.add(new FloatBox(bounds,wrap));}
            public void noteReferenced(String noteId) {
                if(continuous||page.notes.contains(noteId)||info.endnotes.contains(noteId)||!doc.parts().notes().containsKey(noteId)) return;
                float h=noteFlow(noteId,page.settings.contentWidth(),page.number).height();
                page.noteReserve+=h+(page.notes.isEmpty()?10:0);page.notes.add(noteId);
            }
            public int pageNumber(){return page.number;}
            public Region region(){return Region.BODY;}
        }
    }

    private record FloatBox(Rectangle2D.Float bounds,WordPlacement.Wrap wrap) {}

    private static final class PageBuilder {
        final int index,number;final WordPageSettings settings;final boolean sectionStart;
        final List<Line> lines=new ArrayList<>();final List<Decoration> decorations=new ArrayList<>();final List<ObjectBox> objects=new ArrayList<>();
        final List<CellBox> cells=new ArrayList<>();final List<FloatBox> floats=new ArrayList<>();final List<String> notes=new ArrayList<>();
        float bodyTop,bodyBottom,y,noteReserve;int column;
        PageBuilder(int index,int number,WordPageSettings settings,boolean sectionStart){this.index=index;this.number=number;this.settings=settings;this.sectionStart=sectionStart;}
    }

    private static final class Collector implements WordLayoutFlow {
        private final PageBuilder page;private final Region region;
        Collector(PageBuilder page,Region region){this.page=page;this.region=region;}
        public float y(){return 0;}
        public void setY(float y){}
        public float left(){return page.settings.left();}
        public float width(){return page.settings.contentWidth();}
        public float top(){return 0;}
        public boolean paginated(){return false;}
        public boolean atTop(){return true;}
        public float remaining(){return Float.MAX_VALUE;}
        public void breakPage(boolean columnOnly){}
        public float[] slot(float top,float height,float x,float width){return new float[]{x,width,Float.NaN};}
        public void addLine(Line line){page.lines.add(line.as(region,false));}
        public void addObject(ObjectBox box){page.objects.add(box.as(region,false));}
        public void addDecoration(Decoration decoration){page.decorations.add(decoration);}
        public void addCell(CellBox cell){}
        public void addFloat(Rectangle2D.Float bounds,WordPlacement.Wrap wrap){}
        public void noteReferenced(String noteId){}
        public int pageNumber(){return page.number;}
        public Region region(){return region;}
    }
}
