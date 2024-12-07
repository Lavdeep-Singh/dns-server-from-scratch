import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DNSQueryHandler {
    private ByteBuffer queryPacketBuffer;
    private ByteBuffer responsePacketBuffer;

    DNSQueryHandler(DatagramPacket packet){
        byte[] data = packet.getData(); //retrieves the raw byte array from the UDP DatagramPacket that contains the incoming DNS query data.
        queryPacketBuffer = ByteBuffer.wrap(data); // creates a wrapper around those raw bytes that lets us read them as different data types (bytes, shorts, ints) and keep track of our position while reading
        responsePacketBuffer = ByteBuffer.allocate(512);
    }

    byte[] resolveQuery(){

        short questionCountFromHeader = parseAndCreateResponseHeader(responsePacketBuffer, queryPacketBuffer); //get response header

        List<byte[]> domains = new ArrayList<>();
        for(int i=0; i<questionCountFromHeader; i++){
            byte[] domain = copyQuestionSection(queryPacketBuffer, responsePacketBuffer);
            domains.add(domain);
        }

        // Forward each domain query and collect responses
        String resolver = Config.getConfig("resolver");
        if(resolver != null){
            DNSForwarder forwarder = new DNSForwarder(resolver);
            for (byte[] domain : domains) {
                byte[] singleQuery = createSingleDomainQuery(domain);
                byte[] resolverResponse = forwarder.forwardQuery(singleQuery);
                copyAnswerSection(ByteBuffer.wrap(resolverResponse), responsePacketBuffer);
            }
        }else{
            for (byte[] domain : domains) {
                resolveAndWriteAnswerSection(responsePacketBuffer, domain);
              }
        }

        byte[] response = new byte[responsePacketBuffer.position()];

        /*
        * The flip() method in ByteBuffer is crucial for switching from write mode to read mode. It does 3 things:
        *   Sets the limit to current position
        *   Sets position back to 0
        *   Discards the mark if one was set
        *
        * Before flip():
        *   position: points to last written byte
        *   limit: buffer capacity (512)
        *
        * After flip():
        *   position: 0 (start)
        *   limit: previous position
        *
        * This prepares the buffer for reading all the data we just wrote. Without flip(),
        * we'd try reading from the end of our written data rather than the beginning.
        */
        responsePacketBuffer.flip();
        for (int i = 0; i < response.length; i++) {
            response[i] = responsePacketBuffer.get();
        }
        return response;
    }

    private void resolveAndWriteAnswerSection(ByteBuffer responsePacketBuffer,byte[] domain) {
        DNSAnswer answer = new DNSAnswer(domain, "8.8.8.8");
        answer.writeToBuffer(responsePacketBuffer);
  }

    /*
     * This method constructs a DNS query packet for a single domain. 
     * It creates a new ByteBuffer, adds the question section for the specified domain. It sets the query type to "A" (for address records) 
     * and the query class to "IN" (for Internet). The method returns the constructed query packet as a byte array, 
     * which will be forwarded to the resolver.
     */
    private byte[] createSingleDomainQuery(byte[] domain) {
        ByteBuffer queryBuffer = ByteBuffer.allocate(512);

        // Write header with original ID and single question
        short originalId = queryPacketBuffer.getShort(0);
        DNSHeader header = new DNSHeader();
        header.setId(originalId);
        header.setQuestionCount((short) 1);
        header.setRD(true);  // Set recursion desired
        queryBuffer.put(header.toBytes());

        // Write question section
        queryBuffer.put(domain);
        queryBuffer.putShort((short) 1);  // QTYPE = A
        queryBuffer.putShort((short) 1);  // QCLASS = IN

        byte[] result = new byte[queryBuffer.position()];
        queryBuffer.flip();
        queryBuffer.get(result);
        return result;
    }

    private void copyAnswerSection(ByteBuffer source, ByteBuffer dest) {
        // Copy NAME field (2 bytes for compression pointer)
        dest.putShort(source.getShort());
        
        // Copy TYPE (2 bytes)
        dest.putShort(source.getShort());
        
        // Copy CLASS (2 bytes)
        dest.putShort(source.getShort());
        
        // Copy TTL (4 bytes)
        dest.putInt(source.getInt());
        
        // Copy RDLENGTH (2 bytes)
        short rdLength = source.getShort();
        dest.putShort(rdLength);
        
        // Copy RDATA (IPv4 address - 4 bytes)
        byte[] rdata = new byte[rdLength];
        source.get(rdata);
        dest.put(rdata);
    }

    private byte[] copyQuestionSection(ByteBuffer queryPacket, ByteBuffer responsePacket){
        /*
         * ByteArrayOutputStream is a useful class for dynamically building a byte array. (dynamic byte[])
         *   write() - adds a byte to the internal buffer
         *   toByteArray() - converts all written bytes into a byte array (byte[])
         * Think of ByteArrayOutputStream like a growable bucket for bytes - you can keep adding bytes and then get them all at once when needed.
         */
        ByteArrayOutputStream domainBytesArray = new ByteArrayOutputStream();

        /*
        * call position(n) - it moves the pointer to that position (it moves the pointer to that absolute position in the buffer, regardless of where it was before)
        * call get() - it reads one byte and advances the pointer
        * call getShort() - it reads two bytes and advances the pointer
        */
        queryPacket.position(12); //skip header

        /*
        * why if(labelLength == 0) break ?
        * For example, "codecrafters.io" is encoded as:
        * \x0ccodecrafters (length 12 + "codecrafters")
        * \x02io (length 2 + "io")
        * \x00 (null terminator)
        * The loop continues reading labels until it hits the null byte (labelLength == 0),
        * which signals the end of the domain name.
        * This is the standard DNS protocol way of encoding domain names as described in RFC 1035.
        */
        /*
        * DNS Label Compression Format
        * --------------------------
        * Compression uses pointers to reuse previously occurring domain name parts
        * 
        * Pointer Format:
        * - First 2 bits set to 1 (0xC0)
        * - Remaining 14 bits contain offset to referenced name
        * 
        * Example:
        * Original Questions:
        * Q1: codecrafters.io
        * Q2: api.codecrafters.io
        * 
        * Wire Format:
        * Q1: 0x0ccodecrafters0x02io0x00
        * Q2: 0x03api0xC00C
        *
        * Breaking down 0xC00C:
        * - 0xC0: Signals this is a pointer
        * - 0x0C: Offset 12 (points to "codecrafters.io" in Q1)
        * 
        * Decompression Process:
        * 1. Read bytes normally until 0xC0 encountered
        * 2. When found 0xC0, next byte is offset
        * 3. Jump to offset position and read name
        * 4. Return to original position after compressed part
        * 
        * This saves space by reusing common domain parts
        * Max offset is 16383 (14 bits)
        */
        /*
        * DNS Multiple Questions Example
        * ----------------------------
        * Example Query Packet Structure:
        * 
        * HEADER:
        * - Question Count (QDCOUNT) = 2
        * - Other fields as normal...
        * 
        * QUESTION 1 (Uncompressed):
        * Name: codecrafters.io
        * Wire format: 
        *   0x0c (length) + "codecrafters" 
        *   0x02 (length) + "io" 
        *   0x00 (terminator)
        * Type: 0x0001 (A record)
        * Class: 0x0001 (IN)
        * 
        * QUESTION 2 (Compressed):
        * Name: api.codecrafters.io
        * Wire format:
        *   0x03 (length) + "api" 
        *   0xC00C (pointer to "codecrafters.io" at offset 12)
        * Type: 0x0001 (A record)
        * Class: 0x0001 (IN)
        * 
        * Total bytes breakdown:
        * - Header: 12 bytes
        * - Q1: 16 bytes (12 + 2 + 2 for name + type + class)
        * - Q2: 8 bytes (3 + 2 + 1 + 2 for "api" + pointer + type + class)
        */


        while(true){
            byte labelLength = queryPacket.get(); //length of domain/label
            responsePacket.put(labelLength);
            domainBytesArray.write(labelLength);

            // Handle compression pointer
            if ((labelLength & 0xC0) == 0xC0) {
                byte offsetByte = queryPacket.get();
                int offset = ((labelLength & 0x3F) << 8) | (offsetByte & 0xFF);

                // Save position and jump to offset
                queryPacket.position(offset);
                continue;
            }

            if (labelLength == 0) break;

            // Instead of creating a byte array
            for (int i = 0; i < labelLength; i++) {
                byte labelByte = queryPacket.get();
                responsePacket.put(labelByte);
                domainBytesArray.write(labelByte);
            }

        }

        responsePacket.putShort(queryPacket.getShort()); //QTYPE (2 Bytes)
        responsePacket.putShort(queryPacket.getShort()); //QCLASS (2 Bytes)

        return domainBytesArray.toByteArray(); //dynamic byte[] stream => byte[]

    }

    private short parseAndCreateResponseHeader(ByteBuffer responsePacket, ByteBuffer queryPacket){
        // Get id
        short id = queryPacket.getShort();
        // Get flags from query
        byte queryFlags1 = queryPacket.get();
        int oc = (queryFlags1 >> 3) & 0b00001111;
        boolean rd = (queryFlags1 & 0b00000001) != 0;
        // Get RC (last 4 bits of flags2)
        int rc = (oc == 0) ? 0 : 4;  // 0 for standard query, 4 for not implemented
        queryPacket.get(); // skip flags2
        // Read question count (2 Bytes)
        short questionCount = queryPacket.getShort();

        //Create response packet's header
        DNSHeader header = new DNSHeader();
        header.setId(id);
        header.setOC(oc);
        header.setRD(rd);
        header.setRC(rc);
        header.setQuestionCount(questionCount);
        header.setAnswerCount(questionCount);

        responsePacket.put(header.toBytes());
        return questionCount;
    }
}