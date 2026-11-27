package es.valledecernego.game;

/** Deterministic grayscale atlas for the GLES2 BOX60 compatibility avatar. */
public final class PlayerMaterialAtlas {
    public static final int TILE_COUNT=6;
    public static final int TILE_SIZE=16;
    public static final int WIDTH=TILE_COUNT*TILE_SIZE;
    public static final int HEIGHT=TILE_SIZE;
    private PlayerMaterialAtlas() { }

    public static int tileForStyle(int style) {
        return Math.max(0,Math.min(TILE_COUNT-1,style));
    }

    /** ARGB row-major pixels; the GL upload converts these to RGBA bytes. */
    public static int[] pixels() {
        int[] pixels=new int[WIDTH*HEIGHT];
        for(int tile=0;tile<TILE_COUNT;tile++){
            for(int y=0;y<TILE_SIZE;y++){
                for(int x=0;x<TILE_SIZE;x++){
                    int grain=((x*17+y*31+tile*13+x*y*7)%11)-5;
                    int shade;
                    switch(tile){
                        case PlayerModelSpec.MATERIAL_FABRIC: shade=((x+y)%5==0?221:246)+grain; break;
                        case PlayerModelSpec.MATERIAL_COTTON: shade=(y%4==0?232:250)+grain; break;
                        case PlayerModelSpec.MATERIAL_LEATHER: shade=(y%5==0?216:238)+grain; break;
                        case PlayerModelSpec.MATERIAL_SKIN: shade=245+grain/2; break;
                        case PlayerModelSpec.MATERIAL_METAL: shade=((x%4==0||y%4==0)?210:250)+grain; break;
                        default: shade=244+grain; break;
                    }
                    shade=Math.max(0,Math.min(255,shade));
                    int rgb=(shade<<16)|(shade<<8)|shade;
                    pixels[y*WIDTH+tile*TILE_SIZE+x]=0xff000000|rgb;
                }
            }
        }
        return pixels;
    }
}
