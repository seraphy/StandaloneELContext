package jp.seraphyware.sample.standaloneELContext;

import java.awt.Color;
import java.beans.FeatureDescriptor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;


/**
 * ELResolverで独自の"Color"という仮想のルート要素から、
 * ${Color.red}のように色オブジェクト(Color)を返せるようにするリゾルバ.<br>
 * 最初の"Color"要素ではマーカーとなるダミーオブジェクトを返し、
 * 次に、ベースが、このマーカーオブジェクトの場合に、プロパティを色名として判定する.<br>
 * このリゾルバが返すマーカーオブジェクトを標準のBeanELResolverが解決する前に
 * 解釈する必要があるため、ELResolverの順序は、それよりも前になければならない.<br>
 */
public class ColorELResolver extends ELResolver {

	/**
	 * このELResolverが返すマーカー用のダミーオブジェクト.<br>
	 */
	private static final class ColorMarkerObject {
	};

	@Override
	public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
		if (base == null) {
			return String.class;
		}
		return null;
	}
	
	@Override
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base) {
		return Collections.<FeatureDescriptor>emptyList().iterator();
	}
	
	@Override
	public Class<?> getType(ELContext elContext, Object base, Object property) {
		if (elContext == null) {
			throw new NullPointerException();
		}
		if (base == null) {
			elContext.setPropertyResolved(true);
			return ColorMarkerObject.class;

		} else if (base instanceof ColorMarkerObject) {
			return Color.class;
		}
		
		return null;
	}
	
	@Override
	public Object getValue(ELContext elContext, Object base, Object property) {
		if (elContext == null) {
			throw new NullPointerException();
		}
		if (base == null && property instanceof String) {
			String name = (String) property;
			if ("Color".equals(name)) {
				elContext.setPropertyResolved(true);
				return new ColorMarkerObject();
			}
		} else if (base instanceof ColorMarkerObject) {
			String name = (String) property;
			try {
				Field field = Color.class.getField(name);
				Color color = (Color) field.get(null);

				elContext.setPropertyResolved(true);
				return color;
				
			} catch (Exception ex) {
				// 無視
			}
		}
		return null;
	}
	
	@Override
	public boolean isReadOnly(ELContext arg0, Object arg1, Object arg2) {
		return true;
	}
	
	@Override
	public void setValue(ELContext arg0, Object arg1, Object arg2, Object arg3) {
		throw new UnsupportedOperationException("代入はサポートされていません");
	}
}
