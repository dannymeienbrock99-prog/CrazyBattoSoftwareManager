package de.crazybatto.solelink

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BluetoothPermissions {
    fun runtimePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasAll(context: Context): Boolean =
        runtimePermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }

    fun isBluetoothEnabled(context: Context): Boolean {
        // Android 12+ protects BluetoothAdapter.isEnabled with BLUETOOTH_CONNECT.
        // A fresh install has not received that permission yet, so accessing the
        // adapter too early can terminate the Activity before the first screen appears.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasAll(context)) {
            return false
        }

        return try {
            val manager = context.getSystemService(BluetoothManager::class.java)
            manager?.adapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
    }
}
