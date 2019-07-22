package kis.vr180imageviewer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 *
 * @author naoki
 */
public class Vr180Decoder {
    private static final short JPEG_SOI = (short)0xffd8;
    private static final short JPEG_APP1 = (short)0xffe1;
    private static final short JPEG_SOS = (short)0xffda;
    private static final String JPEG_EXT_HEADER =
            "http://ns.adobe.com/xmp/extension/";
    
    public static BufferedImage[] decode(InputStream input) throws IOException {
        var data = input.readAllBytes();
        var bb = ByteBuffer.wrap(data)
                           .order(ByteOrder.BIG_ENDIAN);
        
        var jpegHeader = bb.getShort();
        if (jpegHeader != JPEG_SOI) {
            throw new IOException("The input is not a JPEG ");
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
        
        for (int match = 0; of < gimagedata.length && match < attrname.length; ) {
            if (gimagedata[of++] != attrname[match++]) {
                match = 0;
            }
        }
        // skip to "
        int eof = of;
        for (; gimagedata[eof] != '"'; ++eof){}
        
        // decode
        var b64 = Arrays.copyOfRange(gimagedata, of, eof);
        var rightData = Base64.getDecoder().decode(b64);
        
        // create image
        var leftImage = ImageIO.read(new ByteArrayInputStream(data));
        var rightImage = ImageIO.read(new ByteArrayInputStream(rightData));
        return new BufferedImage[]{leftImage, rightImage};
    }
}
