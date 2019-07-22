package kis.vr180imageviewer;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 *
 * @author naoki
 */
public class ImageViewer {
    
    public static void main(String[] args) throws Exception {
        var file = "W:\\VR180\\20180729JVMLS\\20180803-075356768.vr.jpg";
        var path = Path.of(file);
        
        BufferedImage[] result;
        try (var is = Files.newInputStream(path)) {
            result = Vr180Decoder.decode(is);
        }
        
        // create image
        var rightImage = result[1];
        var leftImage = result[2];
        //ImageIO.write(rightImage, "jpeg", new File("safeway_right.jpg"));
        //ImageIO.write(leftImage, "jpeg", new File("safeway_left.jpg"));
        
        // show window
        var f = new JFrame("VR180");
        f.setLayout(new GridLayout());
        f.add(new JLabel(new ImageIcon(toSmall(leftImage))));
        f.add(new JLabel(new ImageIcon(toSmall(rightImage))));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(500, 500);
        f.setVisible(true);
                
    }
    
    static Image toSmall(Image img) {
        var small = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        var g = small.createGraphics();
        g.drawImage(img, 0, 0, 400, 400, null);
        return small;
    }
    
}
