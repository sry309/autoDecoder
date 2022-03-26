package burp;

import static burp.IndexautoDecoder.DecodeParams;
import static com.autoDecoder.util.codeDecode.decryptKeyivmode;
import static com.autoDecoder.util.codeEncode.encryptKeyivmode;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class BurpExtender extends AbstractTableModel  implements IBurpExtender, ITab, IMessageEditorController,IMessageEditorTabFactory ,IHttpListener{

    private static IBurpExtenderCallbacks callbacks;

    public static boolean isDecoded;

    public static boolean ishost;

    public static boolean flag;

    private IExtensionHelpers helpers;

    private PrintWriter stdout;

    private IndexautoDecoder indexautoDecoder;

    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        this.stdout = new PrintWriter(callbacks.getStdout(), true);
        callbacks.setExtensionName("autoDecoder");
        this.stdout.println("=======================================");
        this.stdout.println("[+]          load successful!          ");
        this.stdout.println("[+]         autoDecoder v0.1           ");
        this.stdout.println("[+]            code by f0ng            ");
        this.stdout.println("[+] https://github.com/f0ng/autoDecoder");
        this.stdout.println("=======================================");
        this.indexautoDecoder = new IndexautoDecoder(); // 创建GUI界面类

        BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this); // 注册 ITab 接口
        BurpExtender.this.callbacks.registerHttpListener(BurpExtender.this); // 注册 HttpListener 接口
        BurpExtender.this.callbacks.registerMessageEditorTabFactory((IMessageEditorTabFactory) BurpExtender.this); // 注册 ITab 接口

    }


    @Override
    public String getTabCaption() {
        return "autoDecoder";
    }

    @Override
    public Component getUiComponent() {
        return indexautoDecoder.$$$getRootComponent$$$();
    }

    @Override
    public IHttpService getHttpService() {
        return null;
    }

    @Override
    public byte[] getRequest() {
        return new byte[0];
    }

    @Override
    public byte[] getResponse() {
        return new byte[0];
    }

    @Override
    public int getRowCount() {
        return 0;
    }

    @Override
    public int getColumnCount() {
        return 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return null;
    }


    @Override
    public IMessageEditorTab createNewInstance(IMessageEditorController iMessageEditorController, boolean b) {
        return new iMessageEditorTab();
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse iHttpRequestResponse) { //处理请求包
        // 当proxy、intruder、repeater模块的时候调用加解密
            if ( (toolFlag == IBurpExtenderCallbacks.TOOL_REPEATER ) || (toolFlag == IBurpExtenderCallbacks.TOOL_INTRUDER ) || (toolFlag == IBurpExtenderCallbacks.TOOL_PROXY )){
            if( (IndexautoDecoder.getRadioButton1State() || IndexautoDecoder.getRadioButton2State()) && messageIsRequest ) { // 请求的时候

                isDecoded = false;
                ishost = false;

                // 如果是明文，就加密以后发出请求，然后响应包也为明文
                byte[] request = iHttpRequestResponse.getRequest();
                IRequestInfo iRequestInfo = helpers.analyzeRequest(iHttpRequestResponse);
                // 获取请求中的所有参数
                List<String> headersList = iRequestInfo.getHeaders();
                int bodyOffset = iRequestInfo.getBodyOffset();
                byte[] body = Arrays.copyOfRange(request, bodyOffset, request.length);

                String[] hosts = IndexautoDecoder.getEncryptHosts();
                for (String header : headersList) {
                    for (String host : hosts)
                        if (header.endsWith(host.replace("*", "").replaceAll(":(.*)",""))) {
                            ishost = true; //返回true
                        }
                }
                if ((new String(body).contains("\"") || new String(body).contains(":")) && ishost) {
                    // todo 当请求体 里有"或者:的时候默认为明文，但是可能不能兼容所有情况
                    isDecoded = true;
                    String decodeBody = null;

                    if (IndexautoDecoder.getRadioButton1State()) { // 如果选中 通过加解密算法进行加解密
                    try {

                        decodeBody = encryptKeyivmode(new String(body), DecodeParams[5], DecodeParams[6], DecodeParams[0], DecodeParams[1], DecodeParams[2], DecodeParams[3], DecodeParams[4]);

                    } catch (Exception e) {
                        e.printStackTrace(); }
                    byte[] httpmsg = helpers.buildHttpMessage(headersList, decodeBody.getBytes());
                    iHttpRequestResponse.setRequest(httpmsg);
                    }

                    if (IndexautoDecoder.getRadioButton2State()) { // 如果选中 通过接口进行加解密
                        System.out.println(new String(body));
                        String decodeTotal = sendPost(IndexautoDecoder.getEncodeApi(),"data="+ new String(body));
                        byte[] httpmsgresp = helpers.buildHttpMessage(headersList, decodeTotal.getBytes());
                        iHttpRequestResponse.setRequest(httpmsgresp);
                    }

            }
            }else if((IndexautoDecoder.getRadioButton2State() || IndexautoDecoder.getRadioButton1State()) && !messageIsRequest){ // 响应的时候，如果是以明文进行请求，那么响应包也为明文
//                System.out.println(isDecoded);
                if ( isDecoded && ishost ) {
                    System.out.println(isDecoded);
                    String decodeBody = null ;
                    byte[] response = iHttpRequestResponse.getResponse();
                    IResponseInfo iResponseInfo = helpers.analyzeResponse(response);
                    // 获取请求中的所有参数
                    List<String> headersList = iResponseInfo.getHeaders();
                    int bodyOffset = iResponseInfo.getBodyOffset();
                    byte[] body = Arrays.copyOfRange(response, bodyOffset, response.length);

                    if (IndexautoDecoder.getRadioButton1State()) { // 如果选中 通过加解密算法进行加解密
                        try {
                            decodeBody = decryptKeyivmode(new String(body), DecodeParams[5], DecodeParams[6], DecodeParams[0], DecodeParams[1], DecodeParams[2], DecodeParams[3], DecodeParams[4]);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        byte[] httpmsgresp = helpers.buildHttpMessage(headersList, decodeBody.getBytes());
                        iHttpRequestResponse.setResponse(httpmsgresp);
                    }
                    if (IndexautoDecoder.getRadioButton2State()) { // 如果选中 通过接口进行加解密
                        String decodeTotal = sendPost(IndexautoDecoder.getDecodeApi(),"data="+ new String(body));
                        byte[] httpmsgresp = helpers.buildHttpMessage(headersList, decodeTotal.getBytes());
                        iHttpRequestResponse.setResponse(httpmsgresp);
                    }
                }
            }

        }

    }


    class iMessageEditorTab implements IMessageEditorTab{
        //实例化iTextEditor返回当前加密数据显示的组件包括加密数据内容
        public ITextEditor iTextEditor = callbacks.createTextEditor();

        //设置消息编辑器的标题
        public String getTabCaption(){
            return "autoDecoder";
        }
        //设置组件 这里直接设置为默认的iTextEditor组件
        public Component getUiComponent(){
            return iTextEditor.getComponent();
        }

        //过滤对特定的请求才生成消息编辑器 当burp中捕捉的请求不符合
        //我们需要的则不生成消息编辑器
        //比如 消息中包含“param”字段、host为www.test.com才生成消息编辑器
        //则如果请求包含它们返回true
        public boolean isEnabled(byte[] content,boolean isRequest){
            //参数content byte[]即是getMessage中获取的iTextEditor中的文本
            //参数isRequest boolean即表示当前文本是request请求 还是 response接收的数据
            //当isRequest true表示request false表示response

            try{
                if (!(IndexautoDecoder.getRadioButton2State() || IndexautoDecoder.getRadioButton1State()))
                    return false;

                if(isRequest){// 判断当请求为request
                    flag = false;
                    IRequestInfo requestInfo = helpers.analyzeRequest(content);
                    List<String> headersList = requestInfo.getHeaders();
                    String[] hosts = IndexautoDecoder.getEncryptHosts();
                    for(String header : headersList) {
//                        System.out.println(header);
                        //请求头中包含指定域名“applog.xx.cn”才设置我们生成的消息编辑器
                        for ( String host:hosts)
                        if(header.endsWith(host.replace("*.",""))) {
                            flag = true; //返回true
                            break;
                        }
                    }
                }else{ // 响应包也需要解密模块
                    if (flag)
                        return true;
                }
            }catch(Exception e){
                e.printStackTrace();
                flag = false;
            }
            return flag;
        }

        //我们要在消息编辑器中显示的消息
        //比如对content解密、添加额外内容、或者替换掉再返回到消息编辑器中
        public void setMessage(byte[] content,boolean isRequest){
            //参数content byte[]即是getMessage中获取的iTextEditor中的文本
            //参数isRequest boolean即表示当前是request请求 还是 response接收的数据
            //当isRequest true表示request false表示response
            try{
                if(isRequest){// 判断当请求为request才处理数据
                    IRequestInfo requestInfo = helpers.analyzeRequest(content);
                    int bodyOffset = requestInfo.getBodyOffset();
                    byte[] body = Arrays.copyOfRange(content, bodyOffset, content.length);
                    List<String> headersList = requestInfo.getHeaders();

                    if (IndexautoDecoder.getRadioButton1State()) { // 如果选中 通过加解密算法进行加解密
                        if (new String(body).contains("\"") || new String(body).contains("'")) {
                            iTextEditor.setText(content);
                        } else {
                            String[] DecodeParams = IndexautoDecoder.getDecodeParams(); // 获取解密数组
                            String decodeBody = decryptKeyivmode(new String(body), DecodeParams[5], DecodeParams[6], DecodeParams[0], DecodeParams[1], DecodeParams[2], DecodeParams[3], DecodeParams[4]);
                            String totalHeaders = "";
                            for (String singleheader : headersList)
                                totalHeaders = totalHeaders + singleheader + "\r\n";
                            iTextEditor.setText((totalHeaders + "\r\n" + decodeBody).getBytes());
                        }
                    }

                    if (IndexautoDecoder.getRadioButton2State()) { // 如果选中 通过接口进行加解密
                        if (new String(body).contains("\"") || new String(body).contains("'")) {
                            iTextEditor.setText(content);
                        } else {
                            String totalHeaders = "";
                            for (String singleheader : headersList)
                                totalHeaders = totalHeaders + singleheader + "\r\n";

                            String decodeTotal = sendPost(IndexautoDecoder.getDecodeApi(), "data=" + new String(body));
                            iTextEditor.setText((totalHeaders + "\r\n" + decodeTotal).getBytes());
                        }
                    }

                }else { // 处理响应包的数据
                    IRequestInfo requestInfo = helpers.analyzeRequest(content);
                    int bodyOffset = requestInfo.getBodyOffset();
                    byte[] body = Arrays.copyOfRange(content, bodyOffset, content.length);
                    List<String> headersList = requestInfo.getHeaders();
                    String[] DecodeParams = IndexautoDecoder.getDecodeParams(); // 获取解密数组
                    if (IndexautoDecoder.getRadioButton1State()) { // 如果选中 通过加解密算法进行加解密
                        String decodeBody = decryptKeyivmode(new String(body), DecodeParams[5], DecodeParams[6], DecodeParams[0], DecodeParams[1], DecodeParams[2], DecodeParams[3], DecodeParams[4]);
                        String totalHeaders = "";
                        for (String singleheader : headersList)
                            totalHeaders = totalHeaders + singleheader + "\r\n";
                        iTextEditor.setText((totalHeaders + "\r\n" + decodeBody).getBytes());
                    }

                    if (IndexautoDecoder.getRadioButton2State()) { // 如果选中 通过接口进行加解密
                        String totalHeaders = "";
                        for (String singleheader : headersList)
                            totalHeaders = totalHeaders + singleheader + "\r\n";

                        String decodeTotal = sendPost(IndexautoDecoder.getDecodeApi(),"data="+ new String(body));
//                        System.out.println(decodeTotal);
                        iTextEditor.setText((totalHeaders + "\r\n" + decodeTotal).getBytes());

                    }

                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        //返回iTextEditor显示的文本
        public byte[] getMessage(){
            return iTextEditor.getText();
        }

        //允许修改消息
        public boolean isModified(){
            return true;
        }

        //返回iTextEditor中选定的文本 没有选择的话 则不返回数据
        public byte[] getSelectedData(){
            return iTextEditor.getSelectedText();
        }
    }

    public static String getPath(){
        String jarPath = callbacks.getExtensionFilename(); // 获取当前jar的路径
        return jarPath.substring(0, jarPath.lastIndexOf("/")) ;
    }

    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }

}