package org.lazywizard.console.overlay.v2.misc

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/*
Made by Lukas04, improved with help from Starficz.
Concepts have been learned from lyravega, float, andylizi and their mods.
**/
internal object ReflectionUtils {

    private val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
    private val setFieldHandle = MethodHandles.lookup().findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Any::class.java, Any::class.java))
    private val getFieldHandle = MethodHandles.lookup().findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
    private val getFieldTypeHandle = MethodHandles.lookup().findVirtual(fieldClass, "getType", MethodType.methodType(Class::class.java))
    private val getFieldNameHandle = MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
    private val setFieldAccessibleHandle = MethodHandles.lookup().findVirtual(fieldClass,"setAccessible", MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType))

    private val methodClass = Class.forName("java.lang.reflect.Method", false, Class::class.java.classLoader)
    private val getMethodNameHandle = MethodHandles.lookup().findVirtual(methodClass, "getName", MethodType.methodType(String::class.java))
    private val getMethodParametersHandle = MethodHandles.lookup().findVirtual(methodClass, "getParameterTypes", MethodType.methodType(arrayOf<Class<*>>().javaClass))
    private val getMethodReturnTypeHandle = MethodHandles.lookup().findVirtual(methodClass, "getReturnType", MethodType.methodType(Class::class.java))

    private val invokeMethodHandle = MethodHandles.lookup().findVirtual(methodClass, "invoke", MethodType.methodType(Any::class.java, Any::class.java, Array<Any>::class.java))


    //Cache
    private var fieldsCache = HashMap<ReflectedFieldKey, ReflectedField>()
    private var fieldNameCache = HashMap<Class<*>, HashSet<String>>()
    private var fieldTypesCache = HashMap<Class<*>, HashSet<Class<*>>>()

    private var methodsCache = HashMap<ReflectedMethodKey, ReflectedMethod>()
    private var methodNameCache = HashMap<Class<*>, HashSet<String>>()

    //Cached Results of Reflected Fields
    private data class ReflectedFieldKey(
        val clazz: Class<*>,
        val fieldName: String?,
        val fieldType: Class<*>?,
        val withSuper: Boolean)
    class ReflectedField(private val field: Any) {
        fun get(instance: Any?): Any? {
            return getFieldHandle.invoke(field, instance)
        }
        fun set(instance: Any?, value: Any?) {
            setFieldHandle.invoke(field, instance, value)
        }
    }

    //Cached Results of Reflected Fields
    private data class ReflectedMethodKey(
        val clazz: Class<*>,
        val methodName: String?,
        val numOfParams: Int?,
        val returnType: Class<*>?,
        val parameterTypes: List<Class<*>?>?) {
    }
    class ReflectedMethod(private val method: Any) {
        fun invoke(instance: Any?, vararg arguments: Any?): Any? = invokeMethodHandle.invoke(method, instance, arguments)
    }

    @JvmStatic
    fun set(fieldName: String? = null, instanceToModify: Any, newValue: Any?, fieldType: Class<*>? = null)
    {
        getField(fieldName, instanceToModify.javaClass, fieldType)!!.set(instanceToModify, newValue)
    }

    @JvmStatic
    fun get(fieldName: String? = null, instanceToGetFrom: Any, fieldType: Class<*>? = null): Any? {
        return getField(fieldName, instanceToGetFrom.javaClass, fieldType)!!.get(instanceToGetFrom)
    }

    fun instantiate(clazz: Class<*>, vararg arguments: Any?) : Any?
    {
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it!!::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        val constructorHandle = MethodHandles.lookup().findConstructor(clazz, methodType)
        val instance = constructorHandle.invokeWithArguments(arguments.toList())

        return instance
    }

    @JvmStatic
    fun invoke(methodName: String? = null, instance: Any, vararg arguments: Any?, returnType: Class<*>? = null, parameterCount: Int? = null) : Any?
    {
        val clazz = instance.javaClass
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }

        var method = getMethod(methodName, instance.javaClass, returnType, parameterCount, args) ?: return null
        return method.invoke(instance, *arguments)
    }

    //Cache for field names to reduce reflection calls during UI Crawling
    fun hasVariableOfName(name: String, instance: Any) : Boolean {
        var clazz = instance.javaClass
        return fieldNameCache.getOrPut(clazz) {
            //Class has not been cached yet, save all method names to the cache
            var set = HashSet<String>()
            val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields as Array<out Any>
            for (field in instancesOfFields) {
                set.add(getFieldNameHandle.invoke(field) as String)
            }
            set //Returns the list
        }.contains(name)
    }

    //Cache for field types to reduce reflection calls during UI Crawling
    fun hasVariableOfType(type: Class<*>, instance: Any) : Boolean {
        var clazz = instance.javaClass
        return fieldTypesCache.getOrPut(clazz) {
            //Class has not been cached yet, save all method names to the cache
            var set = HashSet<Class<*>>()
            val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields as Array<out Any>
            for (field in instancesOfFields) {
                set.add(getFieldTypeHandle.invoke(field) as Class<*>)
            }
            set //Returns the list
        }.contains(type)
    }

    //Cache for method names to reduce reflection calls during UI Crawling
    fun hasMethodOfName(name: String, instance: Any) : Boolean {
        var clazz = instance.javaClass
        return methodNameCache.getOrPut(clazz) {
            //Class has not been cached yet, save all method names to the cache
            var set = HashSet<String>()
            val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods() as Array<out Any>
            for (method in instancesOfMethods) {
                set.add(getMethodNameHandle.invoke(method) as String)
            }
            set //Returns the list
        }.contains(name)
    }

    fun getField(fieldName: String? = null, fieldClazz: Class<*>, fieldType: Class<*>? = null, recursionLimit: Int? = null, superclazz: Class<*>? = null) : ReflectedField? {

        var clazz = fieldClazz
        if (superclazz != null) clazz = superclazz

        var withSuper = false
        if (recursionLimit != null) withSuper = true

        val key = ReflectedFieldKey(clazz, fieldName, fieldType, withSuper)
        return fieldsCache.getOrPut(key) {
            var targetField: Any? = null

            var fields = (clazz.fields + clazz.declaredFields) as Array<out Any>

            for (field in fields) {

                if (fieldName != null) {
                    var name = getFieldNameHandle.invoke(field)
                    if (name != fieldName) continue
                }

                if (fieldType != null) {
                    var type = getFieldTypeHandle.invoke(field)
                    if (type != fieldType) continue
                }

                targetField = field
            }

            if (targetField == null && recursionLimit != null && recursionLimit > 0) {
                var superc = clazz.superclass
                if (superc != null) {
                    targetField = getField(fieldName, clazz, fieldType, recursionLimit-1, superc)
                }
            }

            //Should not crash just because it could not find it.
            if (targetField == null) {
                return null
            }

            setFieldAccessibleHandle.invoke(targetField, true)
            ReflectedField(targetField)
        }
    }

    fun getMethod(methodName: String? = null, clazz: Class<*>, returnType: Class<*>?=null, parameterCount: Int?=null, parameters: List<Class<*>?>?) : ReflectedMethod? {
        val key = ReflectedMethodKey(clazz, methodName, parameterCount, returnType, parameters)
        return methodsCache.getOrPut(key) {
            var targetMethod: Any? = null

            var methods = ((clazz.methods + clazz.declaredMethods)).toSet() as Set<Any>
            for (method in methods.toSet()) {

                if (methodName != null) {
                    var name = getMethodNameHandle.invoke(method)
                    if (name != methodName) continue
                }

                if (parameters?.isNotEmpty() == true || parameterCount != null) {
                    var methodParams = getMethodParametersHandle.invoke(method) as Array<Class<*>>

                    //Skip if parameters do not match
                    if (parameters?.isNotEmpty() == true) {
                        if (methodParams.any { !parameters.contains(it) }) continue
                    }

                    //Skip if parameters count does not match
                    if (parameterCount != null) {
                        if (methodParams.size != parameterCount) continue
                    }
                }

                if (returnType != null) {
                    var type = getMethodReturnTypeHandle.invoke(method)
                    if (type != returnType) continue
                }


                targetMethod = method
                break
            }

            //Should not crash just because it could not find it.
            if (targetMethod == null) {
                return null
            }

            var method = ReflectedMethod(targetMethod)
            //Returns the method
            method
        }
    }

    //Useful for some classes with just one field
    fun getFirstDeclaredField(instanceToGetFrom: Any): Any? {
        var field: Any? = instanceToGetFrom.javaClass.declaredFields[0]
        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }

}