package exception;

import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

/**
 * 返回主菜单异常实体
 */
public class BackToMainMenuException extends Exception{
    final org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());
    public BackToMainMenuException(String str){
       logger.info(str);
    }
}
