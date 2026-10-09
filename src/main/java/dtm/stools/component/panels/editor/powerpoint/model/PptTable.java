package dtm.stools.component.panels.editor.powerpoint.model;

import java.awt.Color;
import java.util.*;

public record PptTable(List<Double> columns,List<Double> heights,List<List<Cell>> rows) {
    public record Cell(PptText text,Color fill,PptStroke top,PptStroke right,PptStroke bottom,PptStroke left,int rowSpan,int colSpan,boolean covered) {
        public Cell {Objects.requireNonNull(text);Objects.requireNonNull(fill);Objects.requireNonNull(top);Objects.requireNonNull(right);Objects.requireNonNull(bottom);Objects.requireNonNull(left);if(rowSpan<1||colSpan<1)throw new IllegalArgumentException("Invalid span");}
        public Cell withText(PptText value){return new Cell(value,fill,top,right,bottom,left,rowSpan,colSpan,covered);}
        public Cell withFill(Color value){return new Cell(text,value,top,right,bottom,left,rowSpan,colSpan,covered);}
        public Cell withBorder(PptStroke value){return new Cell(text,fill,value,value,value,value,rowSpan,colSpan,covered);}
        public Cell span(int r,int c,boolean hidden){return new Cell(text,fill,top,right,bottom,left,r,c,hidden);}
    }
    public PptTable {
        columns=List.copyOf(columns);heights=List.copyOf(heights);rows=rows.stream().map(List::copyOf).toList();
        int columnCount=columns.size();
        if(columns.isEmpty()||rows.isEmpty()||heights.size()!=rows.size()||rows.stream().anyMatch(r->r.size()!=columnCount))throw new IllegalArgumentException("Invalid table grid");
        for(double size:columns)if(!Double.isFinite(size)||size<=0)throw new IllegalArgumentException("Invalid column width");
        for(double size:heights)if(!Double.isFinite(size)||size<=0)throw new IllegalArgumentException("Invalid row height");
        for(int r=0;r<rows.size();r++)for(int c=0;c<columns.size();c++){Cell cell=rows.get(r).get(c);if(!cell.covered()&&(r+cell.rowSpan()>rows.size()||c+cell.colSpan()>columns.size()))throw new IllegalArgumentException("Span exceeds table");}
    }
    public static PptTable create(int rowCount,int columnCount,double width,double height){
        if(rowCount<1||columnCount<1||rowCount>100||columnCount>100)throw new IllegalArgumentException("Table size must be between 1 and 100");
        PptStroke border=new PptStroke(new Color(180,190,205),1,"solid","none","none");
        List<List<Cell>> cells=new ArrayList<>();
        for(int r=0;r<rowCount;r++){List<Cell> row=new ArrayList<>();for(int c=0;c<columnCount;c++)row.add(new Cell(PptText.plain("",24,Color.BLACK),r==0?new Color(225,235,247):Color.WHITE,border,border,border,border,1,1,false));cells.add(row);}
        return new PptTable(Collections.nCopies(columnCount,width/columnCount),Collections.nCopies(rowCount,height/rowCount),cells);
    }
    public double width(){return columns.stream().mapToDouble(Double::doubleValue).sum();}
    public double height(){return heights.stream().mapToDouble(Double::doubleValue).sum();}
    public PptTable cell(int row,int column,Cell value){List<List<Cell>> copy=new ArrayList<>(rows);List<Cell> cells=new ArrayList<>(copy.get(row));cells.set(column,value);copy.set(row,cells);return new PptTable(columns,heights,copy);}
    public PptTable resize(double width,double height){return new PptTable(columns.stream().map(x->x*width/width()).toList(),heights.stream().map(y->y*height/height()).toList(),rows);}
    public PptTable split(){return new PptTable(columns,heights,rows.stream().map(r->r.stream().map(c->c.span(1,1,false)).toList()).toList());}
    public PptTable split(int row,int column){
        for(int r=0;r<rows.size();r++)for(int c=0;c<columns.size();c++){Cell origin=rows.get(r).get(c);if(!origin.covered()&&r<=row&&c<=column&&r+origin.rowSpan()>row&&c+origin.colSpan()>column){
            List<List<Cell>> copy=mutable(rows);for(int rr=r;rr<r+origin.rowSpan();rr++)for(int cc=c;cc<c+origin.colSpan();cc++)copy.get(rr).set(cc,copy.get(rr).get(cc).span(1,1,false));return new PptTable(columns,heights,copy);
        }}return this;
    }
    public PptTable merge(int row,int column,int rowSpan,int colSpan){
        if(row<0||column<0||rowSpan<1||colSpan<1||row+rowSpan>rows.size()||column+colSpan>columns.size())throw new IllegalArgumentException("Invalid merge range");
        if(rowSpan==1&&colSpan==1)return this;
        List<List<Cell>> copy=mutable(rows);StringBuilder text=new StringBuilder();
        for(int r=0;r<rows.size();r++)for(int c=0;c<columns.size();c++){Cell cell=rows.get(r).get(c);if(cell.covered())continue;boolean intersects=r<row+rowSpan&&c<column+colSpan&&r+cell.rowSpan()>row&&c+cell.colSpan()>column;
            if(intersects&&(r<row||c<column||r+cell.rowSpan()>row+rowSpan||c+cell.colSpan()>column+colSpan))throw new IllegalArgumentException("Select complete merged cells");}
        for(int r=row;r<row+rowSpan;r++)for(int c=column;c<column+colSpan;c++){
            Cell old=rows.get(r).get(c);if(!old.covered()&&!old.text().text().isEmpty()){if(!text.isEmpty())text.append('\n');text.append(old.text().text());}
            boolean first=r==row&&c==column;copy.get(r).set(c,(first?old:old.withText(old.text().withText(""))).span(first?rowSpan:1,first?colSpan:1,!first));
        }
        Cell first=copy.get(row).get(column);copy.get(row).set(column,first.withText(first.text().withText(text.toString())));return new PptTable(columns,heights,copy);
    }
    private static List<List<Cell>> mutable(List<List<Cell>> rows){return rows.stream().map(r->(List<Cell>)new ArrayList<>(r)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));}
    public PptTable insertRow(int index){return gridChange(index,true,true);}
    public PptTable deleteRow(int index){return gridChange(index,true,false);}
    public PptTable insertColumn(int index){return gridChange(index,false,true);}
    public PptTable deleteColumn(int index){return gridChange(index,false,false);}
    private PptTable gridChange(int index,boolean rowAxis,boolean insertion){
        int count=rowAxis?rows.size():columns.size();if(index<0||index>(insertion?count:count-1)||!insertion&&count==1)throw new IllegalArgumentException("Invalid table grid change");
        List<List<Cell>> copy=mutable(split().rows());List<Double> widths=new ArrayList<>(columns),sizes=new ArrayList<>(heights);Cell empty=create(1,1,100,40).rows().getFirst().getFirst();
        if(rowAxis){if(insertion){List<Cell> template=rows.get(Math.min(index,rows.size()-1));copy.add(index,new ArrayList<>(template.stream().map(cell->cell.withText(cell.text().withText("")).span(1,1,false)).toList()));sizes.add(index,height()/rows.size());}else{copy.remove(index);sizes.remove(index);}}
        else{if(insertion){widths.add(index,width()/columns.size());for(List<Cell> row:copy){Cell template=row.get(Math.min(index,row.size()-1));row.add(index,template.withText(template.text().withText("")).span(1,1,false));}}else{widths.remove(index);for(List<Cell> row:copy)row.remove(index);}}
        for(int r=0;r<rows.size();r++)for(int c=0;c<columns.size();c++){
            Cell cell=rows.get(r).get(c);if(cell.covered())continue;int origin=rowAxis?r:c,span=rowAxis?cell.rowSpan():cell.colSpan();
            if(!insertion&&origin==index&&span==1)continue;
            int newOrigin=origin+(insertion?(origin>=index?1:0):(origin>index?-1:0));int newSpan=span;
            if(insertion&&origin<index&&origin+span>index)newSpan++;else if(!insertion&&origin<=index&&origin+span>index)newSpan--;
            int rr=rowAxis?newOrigin:r,cc=rowAxis?c:newOrigin,rs=rowAxis?newSpan:cell.rowSpan(),cs=rowAxis?cell.colSpan():newSpan;
            copy.get(rr).set(cc,cell.span(rs,cs,false));
            for(int y=rr;y<rr+rs;y++)for(int x=cc;x<cc+cs;x++)if(y!=rr||x!=cc)copy.get(y).set(x,copy.get(y).get(x).withText(copy.get(y).get(x).text().withText("")).span(1,1,true));
        }return new PptTable(widths,sizes,copy);
    }
}
