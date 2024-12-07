public class DNSHeader {
    private short id; // sort = 2 bytes = 16bits
    private byte flags1;
    private byte flags2;
    private short qdCount;
    private short anCount;
    private short nsCount;
    private short arCount;

    public DNSHeader() {
        this.id = 1234; // Expected value
        this.flags1 = (byte) 0x80; // QR=1, OPCODE=0, AA=0, TC=0, RD=0
        this.flags2 = 0x00; // RA=0, Z=0, RCODE=0
        this.qdCount = 0;
        this.anCount = 0;
        this.nsCount = 0;
        this.arCount = 0;
    }

    public byte[] toBytes() {
        byte[] header = new byte[12]; // DNS header is always 12 bytes

        // ID (16 bits) - converting short to two bytes
        header[0] = (byte) (id >> 8); // Get high byte (left side first 8 bits)
        header[1] = (byte) id; // Get low byte (remaining right side 8 bits)

        // Flags (2 bytes)
        header[2] = flags1;
        header[3] = flags2;

        // Question count (16 bits)
        header[4] = (byte) (qdCount >> 8);
        header[5] = (byte) qdCount;

        // Answer count (16 bits)
        header[6] = (byte) (anCount >> 8);
        header[7] = (byte) anCount;

        // Authority count (16 bits)
        header[8] = (byte) (nsCount >> 8);
        header[9] = (byte) nsCount;

        // Additional count (16 bits)
        header[10] = (byte) (arCount >> 8);
        header[11] = (byte) arCount;

        return header;
    }

    public void setQuestionCount(short count) {
        this.qdCount = count;
    }

    public void setAnswerCount(short count){
        this.anCount = count;
    }

    public void setId(short id){
        this.id = id;
    }

    public void setOC(int opCode){ //4 bytes
        // OPCODE is bits 1-4 in flags1
        // Clear the OPCODE bits first
        this.flags1 &= 0b10000111;
        // Shift opCode into position and set
        this.flags1 |= (opCode << 3);
    }

    public void setRD(boolean rd) {
        // RD is the least significant bit in flags1
        if (rd) {
            flags1 |= 0b00000001;  // Set bit 0
        } else {
            flags1 &= 0b11111110;  // Clear bit 0
        }
    }

    public void setRC(int rcCode){
        //clear last 4 bits which signifies RC in flags2
        this.flags2 &= 0b11110000;
        flags2 |= rcCode;  // Set RC value
    }
}
