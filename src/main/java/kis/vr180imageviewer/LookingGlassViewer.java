/*
 * LookingGlassViewer
 * Created on 2019/07/23 2:49:31
 */
package kis.vr180imageviewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/**
 *
 * @author naoki
 */
public class LookingGlassViewer {
    public static BufferedImage open(InputStream is) throws IOException {
        BufferedImage[] images = Vr180Decoder.decode(is);
        int w = images[0].getWidth();
        int h = images[0].getHeight();
        int dw = 2560 / 5;
        int dh = 1600 * w / 2560;
        BufferedImage[] smalls = Arrays.stream(images)
                .map(img -> {
                    BufferedImage small = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_RGB);
                    var g = small.createGraphics();
                    g.drawImage(img, 0, (dw - h) / 2, dw, h, null);
                    g.dispose();
                    return small;
                }).toArray(BufferedImage[]::new);
        BufferedImage output = new BufferedImage(2560, 1600, BufferedImage.TYPE_INT_RGB);
        var g = output.createGraphics();
        for (int i = 0; i < dw; ++i) {
            for (int j = 0; j < 5; j++) {
                g.drawImage(smalls[0], i * 5 + j, 0, i * 5 + j + 1, 1600, 
                                       i, 0, i + 1, dh, null);
            }
            /*
            for (int j = 0; j < 22; j++) {
                g.drawImage(smalls[1], i * 5 + j + 23, 0, i * 5 + j + 24, 1600, 
                                       i, 0, i + 1, dh, null);
            }*/
        }
        g.dispose();
        return output;
    }
    public static void main(String[] args) throws IOException {
        var file = "W:\\VR180\\20180729JVMLS\\20180803-075356768.vr.jpg";
        var path = Path.of(file);
        
        BufferedImage result;
        try (var is = Files.newInputStream(path)) {
            result = open(is);
        }
        System.out.println("test");
        ImageIO.write(result, "png", new File("C:\\Users\\naoki\\Desktop\\lg.png"));
    }
}
