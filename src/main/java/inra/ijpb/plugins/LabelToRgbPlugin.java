/**
 * 
 */
package inra.ijpb.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.LabelImages;
import inra.ijpb.util.ColorMaps.CommonLabelMaps;

import java.awt.Color;

/**
 * Creates a new Color Image that associate a given color to each label of the input image.
 * Opens a dialog to choose a colormap, a background color, and a shuffle option. 
 * Preview option is available.
 * Note that when shuffle is activated, result may be different from preview.
 *  
 * @author David Legland
 *
 */
public class LabelToRgbPlugin implements PlugIn {
	
	public enum Colors {
		WHITE("White", 	Color.WHITE), 
		BLACK("Black", 	Color.BLACK), 
		RED("Red", 		Color.RED), 
		GREEN("Green", 	Color.GREEN), 
		BLUE("Blue", 	Color.BLUE);
		
		private final String label;
		private final Color color;

		Colors(String label, Color color) {
			this.label = label;
			this.color = color;
		}
		
		public String toString() {
			return label;
		}
		
		public Color getColor() {
			return color;
		}
		
		public static String[] getAllLabels(){
			int n = Colors.values().length;
			String[] result = new String[n];
			
			int i = 0;
			for (Colors color : Colors.values())
				result[i++] = color.label;
			
			return result;
		}
		
		/**
		 * Determines the operation type from its label.
		 * @throws IllegalArgumentException if label is not recognized.
		 */
		public static Colors fromLabel(String label) {
			if (label != null)
				label = label.toLowerCase();
			for (Colors color : Colors.values()) {
				String cmp = color.label.toLowerCase();
				if (cmp.equals(label))
					return color;
			}
			throw new IllegalArgumentException("Unable to parse Colors with label: " + label);
		}
		
	};


	@Override
	public void run(String arg) {
		ImagePlus imagePlus = IJ.getImage();

		int maxLabel = computeMaxLabel(imagePlus);
		
		// Create a new generic dialog with appropriate options
    	GenericDialog gd = new GenericDialog("Label To RGB");
    	gd.addChoice("Colormap", CommonLabelMaps.getAllLabels(), 
    			CommonLabelMaps.SPECTRUM.getLabel());
    	gd.addChoice("Background", Colors.getAllLabels(), Colors.WHITE.label);
    	gd.addCheckbox("Shuffle", true);
    	gd.showDialog();
		
    	// test cancel  
    	if (gd.wasCanceled()) 
    		return;

    	// Create a new LUT from info in dialog
		String lutName = gd.getNextChoice();
		Color bgColor = Colors.fromLabel(gd.getNextChoice()).getColor();
		boolean shuffleLut = gd.getNextBoolean();

		// Create a new LUT from info in dialog
		byte[][] lut = CommonLabelMaps.fromLabel(lutName).computeLut(maxLabel, shuffleLut);
    	
		// Create a new RGB image from index image and LUT options
		ImagePlus resPlus = LabelImages.labelToRgb(imagePlus, lut, bgColor);
    	
		// dispay result image
		resPlus.copyScale(imagePlus);
		resPlus.show();
    	if (imagePlus.getStackSize() > 1) {
    		resPlus.setSlice(imagePlus.getSlice());
    	}
	}

	/**
	 * Computes the maximum value in the input image or stack, in order to 
	 * initialize colormap with the appropriate number of colors. 
	 */
	private final static int computeMaxLabel(ImagePlus imagePlus) {
		int labelMax = 0;
		int nSlices = imagePlus.getImageStackSize();
		if (nSlices == 1) {
			// process planar integer image
			ImageProcessor baseImage = imagePlus.getProcessor();
			for (int i = 0; i < baseImage.getPixelCount(); i++) {
				labelMax = Math.max(labelMax, baseImage.get(i));
			}
		} else {
			// process 3D integer image
			for (int i = 1; i <= nSlices; i++) {
				ImageProcessor image = imagePlus.getStack().getProcessor(i);
				for (int j = 0; j < image.getPixelCount(); j++) {
					labelMax = Math.max(labelMax, image.get(j));
				}
			}
		}
		
		return labelMax;
	}

}
