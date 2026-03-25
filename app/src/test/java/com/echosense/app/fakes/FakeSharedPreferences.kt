package com.echosense.app.fakes

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = data
    override fun getString(key: String, defValue: String?): String? = data[key] as String? ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = data[key] as Set<String>? ?: defValues
    override fun getInt(key: String, defValue: Int): Int = data[key] as Int? ?: defValue
    override fun getLong(key: String, defValue: Long): Long = data[key] as Long? ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = data[key] as Float? ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key] as Boolean? ?: defValue
    override fun contains(key: String): Boolean = data.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(data)
    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val data: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { temp[key] = values; return this }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { temp[key] = value; return this }
        override fun remove(key: String): SharedPreferences.Editor { temp.remove(key); return this }
        override fun clear(): SharedPreferences.Editor { temp.clear(); return this }
        override fun commit(): Boolean { data.putAll(temp); return true }
        override fun apply() { data.putAll(temp) }
    }
}
