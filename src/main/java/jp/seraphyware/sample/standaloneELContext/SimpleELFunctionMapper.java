package jp.seraphyware.sample.standaloneELContext;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

import javax.el.FunctionMapper;

/**
 * シンプルなEL式で評価することのできる関数の定義.<br>
 * このクラスのstaticメソッドをEL式から関数として呼び出すことができる.<br>
 */
public class SimpleELFunctionMapper extends FunctionMapper {
	@Override
	public Method resolveFunction(String prefix, String localName) {
		if ("fn".equals(prefix)) {
			// fnプレフィックスがある場合は、その関数名を
			// このクラスのstaticメソッドの関数として検索し、あれば、それを用いる.
			Class<?> cls = SimpleELFunctionMapper.this.getClass();
			for (Method m : cls.getMethods()) {
				if (Modifier.isStatic(m.getModifiers())) {
					if (m.getName().equals(localName)) {
						return m;
					}
				}
			}
		}
		return null; // 該当なし
	}

	// 以下、EL式の関数として使用するものの定義 //
	
	/**
	 * EL式から呼び出すことのできる長さを返す関数.<br>
	 * 配列、コレクション、マップの場合は要素数を返す.<br>
	 * それ以外は文字列表現の場合の文字数を返す.<br>
	 * nullは0を返す.<br>
	 * @param arg
	 * @return 長さ
	 */
	public static int length(Object arg) {
		if (arg == null) {
			return 0;
		}
		if (arg.getClass().isArray()) {
			return Array.getLength(arg);
		}
		if (arg instanceof Collection) {
			return ((Collection<?>) arg).size();
		}
		if (arg instanceof Map) {
			return ((Map<?, ?>) arg).size();
		}
		return arg.toString().length();
	}
}
