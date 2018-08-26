package kis.vr180imageviewer;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 *
 * @author naoki
 */
public class ImageViewer {
    private static final short JPEG_SOI = (short)0xffd8;
    private static final short JPEG_APP1 = (short)0xffe1;
    private static final short JPEG_SOS = (short)0xffda;
    private static final String JPEG_EXT_HEADER =
            "http://ns.adobe.com/xmp/extension/";
    
    public static void main(String[] args) throws Exception {
        var file = "kid.vr.jpg";
        var path = Path.of(file);
        var data = Files.readAllBytes(path);
        var bb = ByteBuffer.wrap(data)
                           .order(ByteOrder.BIG_ENDIAN);
        var jpegHeader = bb.getShort();
        if (jpegHeader != JPEG_SOI) {
            System.out.println("The file is not a JPEG ");
            return;
        }

        // read extension data
        Map<String, byte[]> exdata = new HashMap<>();
        for(short marker; (marker = bb.getShort()) != JPEG_SOS; ) {
            var seglen = Short.toUnsignedInt(bb.getShort());
            var current = bb.position();
            if (marker == JPEG_APP1) {
                // get a header
                var buf = new ByteArrayOutputStream();
                for (byte ch; (ch = bb.get()) != 0; ) {
                    buf.write(ch);
                }
                var h = new String(buf.toByteArray());
                if (h.equals(JPEG_EXT_HEADER)) {
                    byte[] b = new byte[32];
                    bb.get(b);
                    var guid = new String(b);
                    var exlen = bb.getInt();
                    var exoff = bb.getInt();
                    var databuf = exdata.computeIfAbsent(guid, key -> new byte[exlen]);
                    int datalen = seglen - 2 - JPEG_EXT_HEADER.length() - 1 - 32 - 4 - 4;
                    bb.get(databuf, exoff, datalen);
                }
            }
            bb.position(current + seglen - 2);
        }             
        
        var gimagedata = exdata.values().stream().findFirst().orElseGet(() -> new byte[0]);

        // skip to Gimage:Data
        var attrname = "GImage:Data=\"".getBytes();
        int of = 0;
        int match = 0;
        for (; of < gimagedata.length;) {
            if (gimagedata[of++] != attrname[match++]) {
                match = 0;
            }
            if (match == attrname.length) {
                break;
            }
        }
        // skip to "
        int eof = of;
        for (; gimagedata[eof] != '"'; ++eof){}
        
        // decode
        var b64 = Arrays.copyOfRange(gimagedata, of, eof);
        var rightData = Base64.getDecoder().decode(b64);
        
        // create image
        var rightImage = ImageIO.read(new ByteArrayInputStream(rightData));
        var leftImage = ImageIO.read(new ByteArrayInputStream(data));
        
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
