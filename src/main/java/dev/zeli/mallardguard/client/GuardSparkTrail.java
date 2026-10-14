package dev.zeli.mallardguard.client;

/** Fixed stationary squares, with lifetime/color/bounds stored once per emission tick. */
final class GuardSparkTrail {
    final double[] points;
    final int[] born;
    final float[] cooling;
    private final double[] bounds;
    final int squaresPerGroup;
    private final int groupCapacity;
    int count,groups;
    private int cursor;
    double minX,minY,minZ,maxX,maxY,maxZ;
    GuardSparkTrail(int squares,int lifetime,int headLifetime) {
        squaresPerGroup=squares;groupCapacity=Math.min(headLifetime,lifetime+1);
        int capacity=squares*groupCapacity;
        points=new double[capacity*3];born=new int[groupCapacity];cooling=new float[groupCapacity];bounds=new double[groupCapacity*6];
    }
    int groupIndex(int chronological) { return (cursor-groups+chronological+groupCapacity)%groupCapacity; }
    void expire(double now,int lifetime) {
        boolean changed=false;
        while(groups>0 && now-born[groupIndex(0)]>=lifetime){groups--;count-=squaresPerGroup;changed=true;}
        if(changed)rebuildBounds();
    }
    void emit(GuardSparkPath path,int tick,int squares,float colorProgress) {
        if(path.totalLength<1.0E-6)return;
        if(squares!=squaresPerGroup)throw new IllegalArgumentException("Trail group size changed after creation.");
        boolean overwrite=groups==groupCapacity;
        if(overwrite){groups--;count-=squaresPerGroup;}
        int group=cursor,b=group*6;
        born[group]=tick;cooling[group]=colorProgress;
        int segment=0;double before=0;
        for(int i=0;i<squares;i++) {
            double distance=path.totalLength*i/squares;
            while(segment+1<path.count && distance>=before+path.lengths[segment])before+=path.lengths[segment++];
            int p=segment*6;
            double mix=Math.max(0,Math.min(1,(distance-before)/path.lengths[segment]));
            double x=path.points[p]+(path.points[p+3]-path.points[p])*mix;
            double y=path.points[p+1]+(path.points[p+4]-path.points[p+1])*mix;
            double z=path.points[p+2]+(path.points[p+5]-path.points[p+2])*mix;
            int slot=(group*squares+i)*3;points[slot]=x;points[slot+1]=y;points[slot+2]=z;
            if(i==0){bounds[b]=bounds[b+3]=x;bounds[b+1]=bounds[b+4]=y;bounds[b+2]=bounds[b+5]=z;}
            else {
                bounds[b]=Math.min(bounds[b],x);bounds[b+1]=Math.min(bounds[b+1],y);bounds[b+2]=Math.min(bounds[b+2],z);
                bounds[b+3]=Math.max(bounds[b+3],x);bounds[b+4]=Math.max(bounds[b+4],y);bounds[b+5]=Math.max(bounds[b+5],z);
            }
        }
        cursor=(cursor+1)%groupCapacity;groups++;count+=squares;
        if(overwrite)rebuildBounds();
        else if(groups==1)setBounds(b);
        else includeBounds(b);
    }
    private void setBounds(int b) {
        minX=bounds[b];minY=bounds[b+1];minZ=bounds[b+2];maxX=bounds[b+3];maxY=bounds[b+4];maxZ=bounds[b+5];
    }
    private void includeBounds(int b) {
        minX=Math.min(minX,bounds[b]);minY=Math.min(minY,bounds[b+1]);minZ=Math.min(minZ,bounds[b+2]);
        maxX=Math.max(maxX,bounds[b+3]);maxY=Math.max(maxY,bounds[b+4]);maxZ=Math.max(maxZ,bounds[b+5]);
    }
    private void rebuildBounds() {
        if(groups==0)return;
        setBounds(groupIndex(0)*6);
        for(int i=1;i<groups;i++)includeBounds(groupIndex(i)*6);
    }
}
