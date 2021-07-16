# 认识TCP/IP协议族

## 概念

一系列协议所组成的一个网络分层模型



## 为什么需要分层？

因为网络的不稳定性，网络传输过程中需要多个路由同时承担数据传递，就算有节点掉线也保证能稳定传送数据。



## 怎么保证数据传输的完整性？

TCP主要提供了检验和、序列号/确认应答、超时重传、最大消息长度、滑动窗口控制等方法实现了可靠性传输。

详细解读可以看此博客：https://zhuanlan.zhihu.com/p/112317245



## TCP/IP的分层模型图：

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/tcp-ip%E5%88%86%E5%B1%82%E6%A8%A1%E5%9E%8B.png)

- Application Layer 应⽤层：万维网(HTTP)、文件传输(FTP)、电子邮件(SMTP)、域名转换(DNS)
- Transport Layer 传输层：TCP、UDP 
- Internet Layer ⽹络层：IP
- Link Layer 数据链路层：以太⽹、Wi-Fi



## TCP的握手模型

通俗的表达：1、听得到吗？2、听得到，你呢？3、我也听到了。然后才开始真正对话 

详细可看博客：https://blog.csdn.net/qzcsu/article/details/72861891

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/TCP%E6%8F%A1%E6%89%8B%E6%A8%A1%E5%9E%8B.gif)



## TCP的挥手模型

通俗的表达：1、老师，下课了。2、好，我知道了，我说完这点。3、好了，说完了，下课吧。4、谢谢老师，老师再见

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/TCP%E6%8C%A5%E6%89%8B%E6%A8%A1%E5%9E%8B.gif)





## TCP、UDP编程步骤

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/TCP%E7%BC%96%E7%A8%8B%E6%AD%A5%E9%AA%A4.jpeg)

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/UDP%E7%BC%96%E7%A8%8B%E6%AD%A5%E9%AA%A4.jpeg)

​	从上面TCP、UDP编程步骤可以看出，UDP 服务器端不需要调用监听(listen)和接收(accept)客户端连接，而客户端也不需要连接服务器端(connect)。UDP协议中，任何一方建立socket后，都可以用sendto发送数据、用recvfrom接收数据，不必关心对方是否存在，是否发送了数据。



## TCP和UDP的使用场景

​	为了实现TCP网络通信的可靠性，增加校验和、序号标识、滑动窗口、确认应答、拥塞控制等复杂的机制，建立了繁琐的握手过程，增加了TCP对系统资源的消耗;TCP的重传机制、顺序控制机制等对数据传输有一定延时影响，降低了传输效率。TCP适合对传输效率要求低，但准确率要求高的应用场景，比如万维网(HTTP)、文件传输(FTP)、电子邮件(SMTP)等。

​	UDP是无连接的，不可靠传输，尽最大努力交付数据，协议简单、资源要求少、传输速度快、实时性高的特点，适用于对传输效率要求高，但准确率要求低的应用场景，比如域名转换(DNS)、远程文件服务器(NFS)等。



## ⻓连接

## 为什么要⻓连接？

​	因为移动⽹络并不在 Internet 中，⽽是在运营商的内⽹，并不具有真正的公⽹ IP，因此当某个 TCP 连接在⼀段时间不通信之后，⽹关会出于⽹络性能考虑⽽关闭这条TCP 连接和公⽹的连接通道，导致这个 TCP 端⼝不再能收到外部通信消息，即 TCP连接被动关闭。



## ⻓连接的实现⽅式

​	⼼跳。即在⼀定间隔时间内，使⽤ TCP 连接发送超短⽆意义消息来让⽹关不能将⾃⼰定义为「空闲连接」，从⽽防⽌⽹关将⾃⼰的连接关闭。





# HTTPS连接

## 架构

ISO七层协议:
`TCP` : 传输层
`TLS` : 会话层
*表示层略*
`HTTP`: 应用层
（`TLS`+`HTTP`=>`HTTPS`）

所以是先进行`3`次握手建立`TCP`,然后`4`次握手建立`TLS`,然后进行`HTTP`数据传输。

如果在`TCP`层抓包的话，里头是`TLS`加密过的数据。（中间人无法知道内容）
如果在`HTTP`层（应用层）收取数据的话，是已经解密过的明文。(但是中间人不太可能在应用层，除非已经嵌入到业务层代码了。)



## HTTPS连接流程图

![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/HTTPS%E5%BB%BA%E7%AB%8B%E8%BF%9E%E6%8E%A5%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg)



![](https://raw.githubusercontent.com/QuiteCoder/MyMdImages/main/HTTPS%E8%BF%9E%E6%8E%A5%E6%A8%A1%E5%9E%8B.jpg)

根据上图，拆解出如下几步：

1. 客户端发起连接请求：`Client Hello`，报文中包含**随机数C、客户端的TLS版本号、密码套件列表、扩展列表**。

2. 服务端收到客户端的`Client Hello` 请求之后，如做出如下响应：

3. 1. 返回给客户端`Server Hello`响应，报文中包含**随机数S、确认后的TLS版本号和使用的密码套件**。
   2. 发送服务器使用的证书。
   3. 发送`Server Hello Done` 表示服务器响应结束。

> ★ 第一次握手到此结束，这次握手过程中，客户端、服务器共享了 随机数C、随机数S、服务端证书。
> ”

1. 客户端收到服务端的响应之后会做如下操作：

2. 1. 验证证书的有效性。
   2. 生成随机数pre-master
   3. 使用证书公钥加密pre-master，将加密结果发送给服务端，这个请求叫做`Client key Exchange`。
   4. 客户端根据 随机数C. 随机数S、per-master 计算出对称加密密钥。
   5. 客户端发送`Change Cipher Spec`请求，表示之后的会话改用密钥加密通信。
   6. 客户端发送`Finshed`，请求，报文包含握手数据的校验信息。



1. 服务端收到`Client key Exchange`请求之后:

2. 1. 使用私钥解密pre-master
   2. 根据 随机数C. 随机数S、per-master 计算出对称加密密钥, 和客户端生成的密钥相同。
   3. 发送`Change Cipher Spec`报文，表示之后会话改用密钥加密通信。
   4. 发送`Finshed`，请求，报文包含握手数据的校验信息。

> ★ 第二次握手到此结束，至此握手中通过`握手数据的校验信息`保证了数据的完整性。通过per-master保证密钥的安全性。通过证书确保发送与接收方的正确性。
> ”
> ★ 问题为什么需要三个随机数?
> ”

不信任客户端或服务器伪随机数的可靠性，为了保证真正的“完全随机”“不可预测”，把三个不可靠的随机数混合起来，那么“随机”的程度就非常高了。

> ★ 存在的隐患?
> ”

安全性取决于私钥的安全性，一旦私钥被黑客获取，将不再有安全性可言。



## TLS和SSL的区别？

```java
TLS 1.0 = SSL 3.1
TLS 1.1 = SSL 3.2
TLS 1.2 = SSL 3.3
```

综上，简单说，它们的区别只是版本更迭而已。

SSL协议,当前版本为3.1(SSL3.1就是TLS1.0)。它已被广泛地用于Web浏览器与服务器之间的身份认证和加密数据传输.它位于TCP/IP协议与各种应用层协议之间，为数据通讯提供安全支持。

SSL协议可分为两层： 

SSL记录协议（SSL Record Protocol）：它建立在可靠的传输协议（如TCP）之上，为高层协议提供数据封装、压缩、加密等基本功能的支持。 

SSL握手协议（SSL Handshake Protocol）：它建立在SSL记录协议之上，用于在实际的数据传输开始前，通讯双方进行身份认证、协商加密算法、交换加密密钥等。



