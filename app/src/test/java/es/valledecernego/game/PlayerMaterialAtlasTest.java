package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerMaterialAtlasTest {
    @Test public void atlasHasSixOpaqueTilesAtExpectedDimensions() {
        int[] pixels=PlayerMaterialAtlas.pixels();
        assertEquals(PlayerMaterialAtlas.WIDTH*PlayerMaterialAtlas.HEIGHT,pixels.length);
        assertEquals(96,PlayerMaterialAtlas.WIDTH);
        assertEquals(16,PlayerMaterialAtlas.HEIGHT);
        for(int pixel:pixels){
            assertEquals(255,(pixel>>>24)&255);
            assertEquals((pixel>>>16)&255,(pixel>>>8)&255);
            assertEquals((pixel>>>8)&255,pixel&255);
        }
    }

    @Test public void materialTilesAreDeterministicAndVisuallyDistinct() {
        int[] first=PlayerMaterialAtlas.pixels();
        assertArrayEquals(first,PlayerMaterialAtlas.pixels());
        for(int tile=1;tile<PlayerMaterialAtlas.TILE_COUNT;tile++){
            boolean different=false;
            for(int y=0;y<PlayerMaterialAtlas.TILE_SIZE&&!different;y++){
                for(int x=0;x<PlayerMaterialAtlas.TILE_SIZE;x++){
                    int a=first[y*PlayerMaterialAtlas.WIDTH+x];
                    int b=first[y*PlayerMaterialAtlas.WIDTH+tile*PlayerMaterialAtlas.TILE_SIZE+x];
                    if(a!=b){different=true;break;}
                }
            }
            assertTrue("Material tile "+tile+" should differ from solid",different);
        }
    }

    @Test public void materialStyleSelectionIsClampedToAtlasBounds() {
        assertEquals(0,PlayerMaterialAtlas.tileForStyle(-1));
        assertEquals(0,PlayerMaterialAtlas.tileForStyle(PlayerModelSpec.MATERIAL_SOLID));
        assertEquals(5,PlayerMaterialAtlas.tileForStyle(PlayerModelSpec.MATERIAL_METAL));
        assertEquals(5,PlayerMaterialAtlas.tileForStyle(99));
    }
}
