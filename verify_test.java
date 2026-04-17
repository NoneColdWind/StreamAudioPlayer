import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class verify_test {
    public static void main(String[] args) {
        String testFile = "src/test/java/cn/ncw/music/stream/StreamAudioPlayerTest.java";
        
        try (BufferedReader br = new BufferedReader(new FileReader(testFile))) {
            String line;
            int lineCount = 0;
            int testCount = 0;
            boolean hasImports = false;
            boolean hasClass = false;
            boolean hasSetup = false;
            boolean hasTeardown = false;
            
            System.out.println("验证测试文件结构...");
            System.out.println("==================================");
            
            while ((line = br.readLine()) != null) {
                lineCount++;
                line = line.trim();
                
                // 检查导入语句
                if (line.startsWith("import ")) {
                    hasImports = true;
                }
                
                // 检查类定义
                if (line.startsWith("class StreamAudioPlayerTest")) {
                    hasClass = true;
                }
                
                // 检查@BeforeEach方法
                if (line.startsWith("@BeforeEach")) {
                    hasSetup = true;
                }
                
                // 检查@AfterEach方法
                if (line.startsWith("@AfterEach")) {
                    hasTeardown = true;
                }
                
                // 检查@Test方法
                if (line.startsWith("@Test")) {
                    testCount++;
                }
            }
            
            System.out.println("文件行数: " + lineCount);
            System.out.println("测试方法数量: " + testCount);
            System.out.println("是否包含导入语句: " + (hasImports ? "是" : "否"));
            System.out.println("是否包含类定义: " + (hasClass ? "是" : "否"));
            System.out.println("是否包含@BeforeEach方法: " + (hasSetup ? "是" : "否"));
            System.out.println("是否包含@AfterEach方法: " + (hasTeardown ? "是" : "否"));
            
            System.out.println("==================================");
            System.out.println("测试文件结构验证完成!");
            
        } catch (IOException e) {
            System.err.println("读取文件时出错: " + e.getMessage());
        }
    }
}
