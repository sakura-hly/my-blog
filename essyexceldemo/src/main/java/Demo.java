import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.context.AnalysisContext;
import com.alibaba.excel.read.event.AnalysisEventListener;
import com.alibaba.excel.support.ExcelTypeEnum;

import java.io.*;

public class Demo {
    public static void main(String[] args) throws FileNotFoundException {
        String path = Demo.class.getClassLoader().getResource("11.xls").getPath();
        String out = "D:\\GitHub\\MyBlog\\essyexceldemo\\src\\main\\resources\\out.txt";
        InputStream inputStream = new FileInputStream(new File(path));
        OutputStream outputStream = new FileOutputStream(new File(out));
        try {
            // 解析每行结果在listener中处理
            ExcelListener listener = new ExcelListener(outputStream);

            ExcelReader excelReader = new ExcelReader(inputStream, ExcelTypeEnum.XLS, null, listener);
            excelReader.read();
        } catch (Exception e) {

        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class ExcelListener extends AnalysisEventListener {
        public ExcelListener(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        private OutputStream outputStream;
        public void invoke(Object object, AnalysisContext context) {
            System.out.println("当前行："+context.getCurrentRowNum());
            System.out.println(object);
            byte[] bytes = (object.toString() + "\n").getBytes();
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void doAfterAllAnalysed(AnalysisContext context) {
            // datas.clear();//解析结束销毁不用的资源
        }
    }
}
