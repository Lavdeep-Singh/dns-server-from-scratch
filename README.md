# Java DNS Server Implementation

A robust DNS server implementation in Java that handles DNS queries with support for both direct resolution and forwarding capabilities.

## 🚀 Features

- Custom DNS query handling and response generation
- Support for multiple DNS questions in a single query
- DNS message compression handling
- Configurable DNS forwarding to upstream resolvers
- IPv4 (A record) resolution
- RFC 1035 compliant implementation

## 🛠️ Technical Highlights

- **Efficient Buffer Management**: Uses Java's ByteBuffer for optimal memory handling
- **DNS Header Processing**: Complete implementation of DNS header fields
- **Question Section Handling**: Supports domain name parsing and compression
- **Answer Section Generation**: Creates proper DNS responses with IPv4 addresses
- **Forwarding Mechanism**: Ability to forward queries to upstream DNS resolvers

## 🔍 Implementation Details

The server handles DNS queries through several key components:

1. **Query Parsing**: Processes incoming UDP packets containing DNS queries
2. **Header Management**: Handles DNS message headers with proper flags and counts
3. **Domain Processing**: Supports domain name encoding/decoding with compression
4. **Response Generation**: Creates properly formatted DNS responses
5. **Forwarding**: Optional forwarding to configurable upstream DNS resolvers

## 💻 Usage

```bash
javac -d out src/main/java/*.java
java -cp out Main
```
## 🔧 Technical Deep Dive
- Handles DNS compression pointers (0xC0) for efficient message encoding
- Supports multiple questions in a single DNS query
- Implements proper byte-level manipulation for DNS protocol compliance
- Uses ByteBuffer for efficient memory management and packet handling
- Supports both direct resolution and forwarding modes

## 📚 References
RFC 1035 - Domain Names Implementation and Specification
DNS Protocol Overview

## 🤝 Contributing
Contributions are welcome! Feel free to submit pull requests or open issues for improvements and bug fixes.