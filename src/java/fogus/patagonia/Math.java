package fogus.patagonia;

public final class Math {
	private static double PI = java.lang.Math.PI;
	
    public static double cleanAngle(double angle) {
        double ret = angle;
    	
    	while (ret < 0) {
    		ret += 2 * Math.PI;
    	}
    	
    	while (ret > 2 * Math.PI) {
    		ret -= Math.PI;
    	}
    	
    	return ret;
    }
    
    
}
