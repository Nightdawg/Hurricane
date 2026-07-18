/* Preprocessed source code */
/* $use: lib/bollar */

package haven.res.gfx.tiles.flavor.wspray;

import haven.*;
import haven.render.*;
import haven.resutil.*;
import haven.res.lib.bollar.*;
import java.util.*;
import java.util.function.*;
import java.nio.*;
import static haven.MCache.tilesz;

@haven.FromResource(name = "gfx/tiles/flavor/wspray", version = 2)
public class WaterSpray implements Tileset.Flavor {
    public static final double THRES = 10;

    public static final Coord[] vscan = new Coord[] {
	Coord.of(-1, -1), Coord.of( 0, -1), Coord.of( 1, -1),
	Coord.of(-1,  0), Coord.of( 0,  0), Coord.of( 1,  0),
	Coord.of(-1,  1), Coord.of( 0,  1), Coord.of( 1,  1),
    };

    public WaterSpray(Tileset trn, Object... args) {
    }

    public void flavor(Buffer buf, Terrain trn, Random seed) {
	Effect fx = buf.datum(Effect.id);
	for(Coord tc : trn.tiles()) {
	    double[] zs = new double[9];
	    for(int i = 0; i < 9; i++)
		zs[i] = trn.map.getfz(tc.add(vscan[i]));

	    if(trn.map.tiler(trn.map.gettile(tc.add(-1, 0))) instanceof WaterTile) {
		if((zs[3] > zs[4]) || (zs[6] > zs[7])) {
		    if(((zs[3] - zs[4]) - (zs[4] - zs[5]) > THRES) ||
		       ((zs[6] - zs[7]) - (zs[7] - zs[8]) > THRES))
			fx.addline(tc.mul(tilesz), tc.add(0, 1).mul(tilesz), zs[4], zs[7],
				   Math.max((zs[3] - zs[4]) - (zs[4] - zs[5]), 0),
				   Math.max((zs[6] - zs[7]) - (zs[7] - zs[8]), 0));
		} else if((zs[5] > zs[4]) || (zs[8] > zs[7])) {
		    if(((zs[5] - zs[4]) - (zs[4] - zs[3]) > THRES) ||
		       ((zs[8] - zs[7]) - (zs[7] - zs[6]) > THRES))
			fx.addline(tc.add(0, 1).mul(tilesz), tc.mul(tilesz), zs[7], zs[4],
				   Math.max((zs[8] - zs[7]) - (zs[7] - zs[6]), 0),
				   Math.max((zs[5] - zs[4]) - (zs[4] - zs[3]), 0));
		}
	    }

	    if(trn.map.tiler(trn.map.gettile(tc.add(0, -1))) instanceof WaterTile) {
		if((zs[1] > zs[4]) || (zs[2] > zs[5])) {
		    if(((zs[1] - zs[4]) - (zs[4] - zs[7]) > THRES) ||
		       ((zs[2] - zs[5]) - (zs[5] - zs[8]) > THRES))
			fx.addline(tc.add(1, 0).mul(tilesz), tc.mul(tilesz), zs[5], zs[4],
				   Math.max((zs[2] - zs[5]) - (zs[5] - zs[8]), 0),
				   Math.max((zs[1] - zs[4]) - (zs[4] - zs[7]), 0));
		} else if((zs[7] > zs[4]) || (zs[8] > zs[5])) {
		    if(((zs[7] - zs[4]) - (zs[4] - zs[1]) > THRES) ||
		       ((zs[8] - zs[5]) - (zs[5] - zs[2]) > THRES))
			fx.addline(tc.mul(tilesz), tc.add(1, 0).mul(tilesz), zs[4], zs[5],
				   Math.max((zs[7] - zs[4]) - (zs[4] - zs[1]), 0),
				   Math.max((zs[8] - zs[5]) - (zs[5] - zs[2]), 0));
		}
	    }

	    if(Math.abs(zs[8] - zs[4]) >= Math.abs(zs[5] - zs[7])) {
		double a = ((zs[4] - zs[7]) + (zs[4] - zs[5])) * 0.5;
		double b = ((zs[7] - zs[8]) + (zs[5] - zs[8])) * 0.5;
		if(a > 0) {
		    if(a - b > THRES)
			fx.addline(tc.add(1, 0).mul(tilesz), tc.add(0, 1).mul(tilesz), zs[5], zs[7],
				   a - b, a - b);
		} else if(b < 0) {
		    if(a - b > THRES)
			fx.addline(tc.add(0, 1).mul(tilesz), tc.add(1, 0).mul(tilesz), zs[7], zs[5],
				   a - b, a - b);
		}
	    }

	    if(Math.abs(zs[5] - zs[7]) > Math.abs(zs[8] - zs[4])) {
		double a = ((zs[5] - zs[4]) + (zs[5] - zs[8])) * 0.5;
		double b = ((zs[4] - zs[7]) + (zs[8] - zs[7])) * 0.5;
		if(a > 0) {
		    if(a - b > THRES)
			fx.addline(tc.add(1, 1).mul(tilesz), tc.mul(tilesz), zs[8], zs[4],
				   a - b, a - b);
		} else if(b < 0) {
		    if(a - b > THRES)
			fx.addline(tc.mul(tilesz), tc.add(1, 1).mul(tilesz), zs[4], zs[8],
				   a - b, a - b);
		}
	    }
	}
    }
}
