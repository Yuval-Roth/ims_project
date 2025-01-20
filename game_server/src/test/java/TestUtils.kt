@file:Suppress("UNCHECKED_CAST")

import org.mockito.Mockito.spy

fun <T> getField(obj: Any, fieldName: String): T {
    val field = obj::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(obj) as T
}

fun setField(obj: Any, fieldName: String, value: Any) {
    val field = obj::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(obj, value)
}

fun <T> spyOnField(obj: Any, fieldName: String): T {
    val field = obj::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    val spy = spy(field.get(obj))
    field.set(obj, spy)
    return spy as T
}