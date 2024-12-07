import java.nio.ByteBuffer;

public class DNSAnswer {

    private byte[] domainName;
    private short type = 1;    // A record (2 bytes)
    private short class_ = 1;  // IN class (2 bytes)
    private int ttl = 60;      // TTL in seconds (4 bytes integer)
    private short rdLength = 4; // Length of IP address (2 bytes)
    private byte[] rdata;      // IP address bytes (4 bytes integer)

    public DNSAnswer(byte[] domainName, String ipAddress){
        this.domainName = domainName;
        this.rdata = convertIPToBytes(ipAddress);
    }

    private byte[] convertIPToBytes(String ipAddString){
        byte[] ip = new byte[4]; //4 bytes array
        String[] parts = ipAddString.split("\\."); // "8.8.8.8" => ["8", "8", "8", "8"]

        for(int i=0; i<4; i++){
            ip[i] = (byte)Integer.parseInt(parts[i]); // "8" => 8 => 0b00001000
            //each byte can represent 128 Integers (0 -> 127)
        }

        return ip;
    }

    public void writeToBuffer(ByteBuffer buffer) {
        buffer.put(domainName);
        buffer.putShort(type);
        buffer.putShort(class_);
        buffer.putInt(ttl);
        buffer.putShort(rdLength);
        buffer.put(rdata);
    }

}
