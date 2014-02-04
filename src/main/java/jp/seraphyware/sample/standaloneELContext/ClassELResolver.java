package jp.seraphyware.sample.standaloneELContext;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;


/**
 * ELResolverで独自の"Class"という仮想のルート要素から、
 * ${Class['java.awt.Color'].red}のようにクラスオブジェクトとstaticフィールドを取得できるようにする。
 * 最初の"Class"要素ではマーカーとなるClassオブジェクトを返し、
 * 次に、ベースが、Classオブジェクトの場合に、クラス名として解決し、そのクラスオブジェクトを返す.<br>
 * 次に、ベースが、Class.class以外のクラスオブジェクトの場合には、プロパティをstaticフィールド名として
 * そのフィールドの値を返す.<br>
 * このリゾルバが返すマーカーオブジェクトを標準のBeanELResolverが解決する前に
 * 解釈する必要があるため、ELResolverの順序は、それよりも前になければならない.<br>
 */
public class ClassELResolver extends ELResolver {

	@Override
	public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
		if (base == null) {
			// baseがnull、つまり最上位であれば"Class"という文字列を受け入れるので文字列型.
			return String.class;
		}
		if (base instanceof Class) {
			// baseがClassであればクラス名またはフィールド名を受け取れるので文字列型
			return String.class;
		}
		return null;
	}

	@Override
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base) {
		if (base == null) {
			return null;
		}
		// 手抜き。本来は選択可能なフィールド名一覧を返すところ.
		return Collections.<FeatureDescriptor>emptyList().iterator();
	}

	@Override
	public Class<?> getType(ELContext elContext, Object base, Object property) {
		if (elContext == null) {
			throw new NullPointerException();
		}
		if (base == null && property instanceof String) {
			String name = (String) property;
			if (name.equals("Class")) {
				elContext.setPropertyResolved(true);
				return Class.class;
			}

		} else if (base instanceof Class) {
			elContext.setPropertyResolved(true);
			if (base.equals(Class.class)) {
				// クラスオブジェクト
				return (Class<?>) base;
			}
			// もしくはフィールドの値
			return Object.class;
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
			if ("Class".equals(name)) {
				// 値が発見されたので、これ以上探索不要であることを通知する.
				elContext.setPropertyResolved(true);
				return Class.class;
			}

		} else if (base instanceof Class) {
			String name = (String) property;
			if (base.equals(Class.class)) {
				// 実行中のスレッドコンテキストのクラスローダを取得する.
				// (なければ、このクラスをロードしたクラスローダーを取得する.)
				ClassLoader cl = AccessController
						.doPrivileged(new PrivilegedAction<ClassLoader>() {
							@Override
							public ClassLoader run() {
								Thread thread = Thread.currentThread();
								ClassLoader cl = thread.getContextClassLoader();
								if (cl != null) {
									return cl;
								}
								return ClassELResolver.class.getClassLoader();
							}
						});
				// クラス名として索引する.
				try {
					Class<?> cls =  Class.forName(name, true, cl);
					elContext.setPropertyResolved(true);
					return cls;

				} catch (ClassNotFoundException e) {
					// 例外は無視する.
				}
				// 他のELResolverを試す必要がないので、ここで例外を出す.
				throw new PropertyNotFoundException("Undefined class name: " + name);
			}

			// フィールド名として検索する.
			try {
				Class<?> cls = (Class<?>) base;
				Field field = cls.getField(name);
				if (Modifier.isStatic(field.getModifiers())) {
					Object value = field.get(null);
					elContext.setPropertyResolved(true);
					return value;
				}

			} catch (Exception ex) {
				// 例外は無視する.
			}
			// 他のELResolverを試す必要がないので、ここで例外を出す.
			throw new PropertyNotFoundException("Undefined field: " + name
					+ "/class=" + base);
		}
		return null; // 他のELResolverを試す.
	}

	@Override
	public boolean isReadOnly(ELContext elContext, Object base, Object property) {
		if (base instanceof Class && property instanceof String) {
			if (base.equals(Class.class)) {
				return false;
			}
			String name = (String) property;

			Class<?> cls = (Class<?>) base;
			try {
				Field field = cls.getField(name);
				int mod = field.getModifiers();
				if (Modifier.isStatic(mod) && !Modifier.isFinal(mod)) {
					// staticで、finalでないフィールドは書き込み可能とみなす.
					return true;
				}

			} catch (Exception ex) {
				// 例外は無視する.
			}
		}
		// 代入はできない.
		return true;
	}

	@Override
	public void setValue(ELContext elContext, Object base, Object property, Object value) {
		if (base instanceof Class && property instanceof String) {
			if (!base.equals(Class.class)) {
				elContext.setPropertyResolved(true);
				Class<?> cls = (Class<?>) base;
				String name = (String) property;
				try {
					Field field = cls.getField(name);
					int mod = field.getModifiers();
					if (Modifier.isStatic(mod) && !Modifier.isFinal(mod)) {
						// フィールドがstaticで、finalでなければ値を設定してみる.
						field.set(null, value);
						return;
					}

				} catch (NoSuchFieldException ex) {
					// フィールド不明の場合は他のELResolverを試す必要がないので、ここで例外を出す.
					throw new PropertyNotFoundException("Undefined field: " + name
							+ "/class=" + base);

				} catch (Exception ex) {
					// そのほかの例外は無視する.
				}
			}
		}
		throw new PropertyNotWritableException("代入はサポートされていません/base="
				+ base + "/property=" + property);
	}
}
