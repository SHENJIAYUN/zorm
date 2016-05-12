package com.zorm.annotations.reflection;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.zorm.util.ReflectHelper;

public class JavaReflectionManager implements ReflectionManager,MetadataProviderInjector{

	private MetadataProvider metadataProvider;
	private final  TypeEnvironmentFactory typeEnvs = new TypeEnvironmentFactory();
	private final Map<TypeKey, JavaXClass> xClasses = new HashMap<TypeKey, JavaXClass>();
	private final Map<MemberKey, JavaXProperty> xProperties = new HashMap<MemberKey, JavaXProperty>();
	
	public MetadataProvider getMetadataProvider() {
        if(metadataProvider == null){
        	setMetadataProvider(new JavaMetadataProvider());
        }
		return metadataProvider;
	}

	public void setMetadataProvider(MetadataProvider metadataProvider) {
		this.metadataProvider = metadataProvider;
	}

	public XClass toXClass(Class clazz) {
		return toXClass(clazz,IdentityTypeEnvironment.INSTANCE);
	}

	XClass toXClass(Type t,final TypeEnvironment context) {
        return new TypeSwitch<XClass>(){
        	@Override
        	public XClass caseClass(Class classType) {
        		//创建类型的key
        	  	TypeKey key = new TypeKey(classType,context);
        	  	JavaXClass result = xClasses.get(key);
        	  	if(result==null){
        	  		result = new JavaXClass(classType, context, JavaReflectionManager.this);
        	  		xClasses.put(key, result);
        	  	}
        	  	return result;
        	}
        	@Override
        	public XClass caseParameterizedType(ParameterizedType parameterizedType){
        		return toXClass(parameterizedType.getRawType(),
        				typeEnvs.getEnvironment(parameterizedType,context));
        	}
        }.doSwitch(context.bind(t));
	}

	private static class TypeKey extends Pair<Type,TypeEnvironment>{
		TypeKey(Type t,TypeEnvironment context) {
          super(t, context);
		}
	}
	
	public Class toClass(XClass xClazz) {
		return null;
	}

	public Method toMethod(XMethod method) {
		return null;
	}

	public <T> XClass classForName(String name, Class<T> caller)
			throws ClassNotFoundException {
		return toXClass(ReflectHelper.classForName(name,caller));
	}

	public XPackage packageForName(String packageName)
			throws ClassNotFoundException {
		return null;
	}

	public <T> boolean equals(XClass class1, Class<T> class2) {
		if ( class1 == null ) {
			return class2 == null;
		}
		return ( (JavaXClass) class1 ).toClass().equals( class2 );
	}

	public AnnotationReader buildAnnotationReader(
			AnnotatedElement annotatedElement) {
		return getMetadataProvider().getAnnotationReader(annotatedElement);
	}

	public Map getDefaults() {
		return null;
	}

	public TypeEnvironment getTypeEnvironment(final Type t) {
		return new TypeSwitch<TypeEnvironment>(){
			@Override
			public TypeEnvironment caseClass(Class classType) {
				return typeEnvs.getEnvironment( classType );
			}
		}.doSwitch(t);
	}

	XProperty getXProperty(Member member, TypeEnvironment typeEnvironment) {
		MemberKey key = new MemberKey(member,typeEnvironment);
		JavaXProperty xProperty = xProperties.get(key);
		if(xProperty == null){
			xProperty = JavaXProperty.create(member, typeEnvironment,this);
			xProperties.put(key, xProperty);
		}
		return xProperty;
	}
	
	private static class MemberKey extends Pair<Member,TypeEnvironment>{

		MemberKey(Member member, TypeEnvironment typeEnvironment) {
			super(member, typeEnvironment);
		}
		
	}

	public TypeEnvironment toApproximatingEnvironment(TypeEnvironment context) {
		return typeEnvs.toApproximatingEnvironment(context);
	}

	public JavaXType toXType(TypeEnvironment context, Type propType) {
        Type boundType = toApproximatingEnvironment(context).bind(propType);
        if(TypeUtils.isArray(boundType)){
        	return new JavaXArrayType(propType,context,this);
        }
        if ( TypeUtils.isCollection( boundType ) ) {
			return new JavaXCollectionType( propType, context, this );
		}
		if ( TypeUtils.isSimple( boundType ) ) {
			return new JavaXSimpleType( propType, context, this );
		}
		throw new IllegalArgumentException( "No PropertyTypeExtractor available for type void " );
	}

}
