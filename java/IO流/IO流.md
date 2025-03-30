## java I/O

Java IO（Input/Output）是 Java 标准库中用于处理输入输出的核心 API。它提供了丰富的类和方法，用于读写数据、操作文件、处理流等。Java IO 主要分为两类：

1. **字节流**：以字节为单位进行数据读写，适用于二进制数据（如图片、音频、视频等）。
2. **字符流**：以字符为单位进行数据读写，适用于文本数据。

------

### **1. Java IO 的核心类**

#### **1.1 字节流**

- **InputStream**：字节输入流的抽象基类。
  - 常用实现类：
    - `FileInputStream`：从文件中读取字节。
    - `ByteArrayInputStream`：从字节数组中读取字节。
    - `BufferedInputStream`：带缓冲区的字节输入流，提高读取效率。
- **OutputStream**：字节输出流的抽象基类。
  - 常用实现类：
    - `FileOutputStream`：向文件中写入字节。
    - `ByteArrayOutputStream`：向字节数组中写入字节。
    - `BufferedOutputStream`：带缓冲区的字节输出流，提高写入效率。

#### **1.2 字符流**

- **Reader**：字符输入流的抽象基类。
  - 常用实现类：
    - `FileReader`：从文件中读取字符。
    - `BufferedReader`：带缓冲区的字符输入流，提高读取效率。
    - `InputStreamReader`：将字节流转换为字符流。
- **Writer**：字符输出流的抽象基类。
  - 常用实现类：
    - `FileWriter`：向文件中写入字符。
    - `BufferedWriter`：带缓冲区的字符输出流，提高写入效率。
    - `OutputStreamWriter`：将字符流转换为字节流。

------

### **2. 使用示例**

#### **2.1 字节流示例**

以下是一个使用字节流读写文件的示例：

```java
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ByteStreamExample {
    public static void main(String[] args) {
        String inputFile = "input.txt";
        String outputFile = "output.txt";

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            // 读取文件并写入到另一个文件
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            System.out.println("File copied successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

------

#### **2.2 字符流示例**

以下是一个使用字符流读写文件的示例：

```java
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CharStreamExample {
    public static void main(String[] args) {
        String inputFile = "input.txt";
        String outputFile = "output.txt";

        try (FileReader fr = new FileReader(inputFile);
             FileWriter fw = new FileWriter(outputFile)) {

            char[] buffer = new char[1024];
            int charsRead;

            // 读取文件并写入到另一个文件
            while ((charsRead = fr.read(buffer)) != -1) {
                fw.write(buffer, 0, charsRead);
            }

            System.out.println("File copied successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

------

#### **2.3 缓冲流示例**

缓冲流可以提高 I/O 操作的效率。以下是一个使用缓冲流的示例：

```java
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BufferedStreamExample {
    public static void main(String[] args) {
        String inputFile = "input.txt";
        String outputFile = "output.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            String line;

            // 逐行读取文件并写入到另一个文件
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine(); // 写入换行符
            }

            System.out.println("File copied successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

------

### **3. Java IO 的使用场景**

- **文件操作**：读写文件、复制文件、删除文件等。
- **网络通信**：通过流处理网络数据。
- **数据处理**：处理二进制数据（如图片、音频）或文本数据（如日志文件）。
- **序列化**：将对象转换为字节流进行存储或传输。

------

### **4. Java IO 的优缺点**

#### **优点**

- 简单易用，适合处理小规模数据。
- 提供了丰富的类和方法，支持多种数据源和目标。

#### **缺点**

- **阻塞式 I/O**：传统的 Java IO 是阻塞式的，不适合高并发场景。
- **性能较低**：对于大规模数据或高并发场景，性能不如 NIO。

------

### **5. Java IO 与 Java NIO 的区别**

| 特性         | Java IO                   | Java NIO               |
| :----------- | :------------------------ | :--------------------- |
| **模型**     | 阻塞式                    | 非阻塞式               |
| **数据单位** | 字节流或字符流            | 缓冲区（Buffer）       |
| **通道**     | 无                        | 支持通道（Channel）    |
| **选择器**   | 无                        | 支持选择器（Selector） |
| **适用场景** | 小规模数据、简单 I/O 操作 | 高并发、大规模数据     |

------

### **6. 总结**

Java IO 是 Java 中处理输入输出的基础 API，适合处理文件操作、网络通信等场景。对于简单的 I/O 操作，Java IO 是一个很好的选择。但对于高并发或大规模数据处理，建议使用 Java NIO 或更高性能的库（如 Netty）。



## NIO

Android NIO（Non-blocking I/O）是 Java NIO 的一部分，提供了一种高效的 I/O 操作方式，特别适合处理网络通信和文件操作。与传统的阻塞 I/O 相比，NIO 支持非阻塞模式，能够更高效地处理多连接场景。

以下是 Android NIO 的核心组件及其用法：

------

### **1. 核心组件**

#### **1.1 Buffer（缓冲区）**

- 用于存储数据，是数据读写的中转站。
- 常见的 Buffer 类型：`ByteBuffer`、`CharBuffer`、`IntBuffer` 等。
- 重要属性：
  - `capacity`：缓冲区的最大容量。
  - `position`：当前读写位置。
  - `limit`：读写操作的边界。
  - `mark`：标记位置，用于 reset。

#### **1.2 Channel（通道）**

- 用于与数据源（如文件、网络套接字）进行数据交互。
- 常见的 Channel 类型：
  - `FileChannel`：文件读写。
  - `SocketChannel`：TCP 客户端。
  - `ServerSocketChannel`：TCP 服务端。
  - `DatagramChannel`：UDP 通信。
- 通道支持阻塞和非阻塞模式。

#### **1.3 Selector（选择器）**

- 用于监听多个通道的事件（如连接、读、写）。
- 允许单线程处理多个通道，适合高并发场景。
- 核心事件：
  - `SelectionKey.OP_ACCEPT`：接受连接。
  - `SelectionKey.OP_CONNECT`：连接就绪。
  - `SelectionKey.OP_READ`：读就绪。
  - `SelectionKey.OP_WRITE`：写就绪。

------

### **2. 使用示例**

#### **2.1 非阻塞 TCP 服务器**

以下是一个简单的 Android NIO TCP 服务器示例：

```java
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioServer {

    public static void main(String[] args) throws IOException {
        // 创建 Selector
        Selector selector = Selector.open();

        // 创建 ServerSocketChannel 并绑定端口
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));
        serverSocketChannel.configureBlocking(false); // 设置为非阻塞模式

        // 注册 ServerSocketChannel 到 Selector，监听 ACCEPT 事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started on port 8080...");

        while (true) {
            // 等待事件发生
            selector.select();

            // 获取已就绪的事件
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                if (key.isAcceptable()) {
                    // 处理新连接
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel client = server.accept();
                    client.configureBlocking(false); // 设置为非阻塞模式

                    // 注册客户端通道到 Selector，监听 READ 事件
                    client.register(selector, SelectionKey.OP_READ);
                    System.out.println("New client connected: " + client.getRemoteAddress());
                }

                if (key.isReadable()) {
                    // 处理读事件
                    SocketChannel client = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int bytesRead = client.read(buffer);

                    if (bytesRead > 0) {
                        buffer.flip(); // 切换为读模式
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        String message = new String(data);
                        System.out.println("Received from client: " + message);

                        // 回显消息给客户端
                        ByteBuffer response = ByteBuffer.wrap(("Echo: " + message).getBytes());
                        client.write(response);
                    } else if (bytesRead == -1) {
                        // 客户端关闭连接
                        System.out.println("Client disconnected: " + client.getRemoteAddress());
                        client.close();
                    }
                }

                iterator.remove(); // 移除已处理的事件
            }
        }
    }
}
```

------

#### **2.2 非阻塞 TCP 客户端**

以下是一个简单的 Android NIO TCP 客户端示例：

```java
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioClient {

    public static void main(String[] args) throws IOException {
        // 创建 SocketChannel
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false); // 设置为非阻塞模式

        // 连接到服务器
        socketChannel.connect(new InetSocketAddress("localhost", 8080));

        // 等待连接完成
        while (!socketChannel.finishConnect()) {
            // 可以在这里做其他事情
        }

        System.out.println("Connected to server...");

        // 发送数据
        String message = "Hello, Server!";
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        socketChannel.write(buffer);

        // 接收服务器的响应
        ByteBuffer responseBuffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(responseBuffer);
        if (bytesRead > 0) {
            responseBuffer.flip();
            byte[] data = new byte[responseBuffer.remaining()];
            responseBuffer.get(data);
            String response = new String(data);
            System.out.println("Received from server: " + response);
        }

        // 关闭连接
        socketChannel.close();
    }
}
```

------

### **3. 使用场景**

- **网络通信**：适合高并发的服务器应用，如聊天服务器、游戏服务器。
- **文件操作**：适合大文件的读写操作，如文件传输。
- **高性能 I/O**：适合需要高效处理多个 I/O 操作的场景。

------

### **4. 注意事项**

- **复杂性**：NIO 的编程模型比传统 I/O 复杂，需要处理缓冲区、通道和选择器的细节。
- **调试难度**：非阻塞模式下，调试和错误处理可能更加困难。
- **性能优化**：在高并发场景下，需要合理配置缓冲区大小和选择器的使用。

通过掌握 Android NIO，你可以构建高性能的 I/O 应用，特别是在需要处理大量并发连接的场景中。