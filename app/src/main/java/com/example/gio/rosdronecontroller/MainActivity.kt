package com.example.gio.rosdronecontroller

import android.app.*
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.server_ip_popup.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), JoystickView.JoystickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private var serverIPAddress: String? = null
    private val KEY_DEFAULT_SERVER_IP_ADDRESS = "default_server_ip_address"

    private var leftJoystickCommand: String? = "00"
    private var rightJoystickCommand: String? = "00"

    private var endCommand: Boolean = false
    private var resetCommand: Boolean = false
    private var armCommand: Boolean = false
    private var disarmCommand: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        serverIPAddress = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(KEY_DEFAULT_SERVER_IP_ADDRESS, "")

        val looper = Looper.getMainLooper()
        val batteryMockupRunnable = object : Runnable {
            override fun run() {
                Handler(looper).post {
                    var per = batteryTextView.text.toString().substring(0, batteryTextView.text.toString().length - 1).toInt()
                    if (per <= 0) {
                        per = 90
                    }
                    per -= 1
                    batteryTextView.text = per.toString() + "%"
                }
            }
        }
        var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        scheduledExecutorService.scheduleAtFixedRate(batteryMockupRunnable, 0, 30000, TimeUnit.MILLISECONDS)

        Log.d("TEXTO", batteryTextView.text.toString().substring(0, batteryTextView.text.toString().length - 1 ))
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_settings -> {
                ServerIPAddressDialogFragment().show(fragmentManager, "serverip_popup")
                return true
            }
            else -> { return super.onOptionsItemSelected(item) }
        }
    }

    class ServerIPAddressDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val inflater = activity.layoutInflater
            var serverIPPopupView = inflater.inflate(R.layout.server_ip_popup, null)

            var builder = AlertDialog.Builder(activity)
            builder.setView(serverIPPopupView)

            var preferenceManager = PreferenceManager.getDefaultSharedPreferences(activity)

            val editTextView = serverIPPopupView.findViewById<EditText>(R.id.serverIPEditText) //serverIPEditText
            if (editTextView != null) {
                editTextView.setText(
                        preferenceManager.getString("default_server_ip_address", ""),
                        TextView.BufferType.EDITABLE)
                editTextView.setSelection(editTextView.text.length)
            }
            //activity.finishAndRemoveTask()

            builder.setPositiveButton("SAVE", DialogInterface.OnClickListener {
                        dialogInterface, i ->
                            Log.d("PREFERENCES", "putting: " + editTextView.text.toString())
                            preferenceManager
                                    .edit()
                                    .putString("default_server_ip_address", editTextView.text.toString())
                                    .apply()
                    })
                    .setNegativeButton("CANCEL", DialogInterface.OnClickListener {
                        dialogInterface, i -> dialog.cancel()
                    })
            return builder.create()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, s: String?) {
        if (s.equals(KEY_DEFAULT_SERVER_IP_ADDRESS)) {
            serverIPAddress = PreferenceManager
                    .getDefaultSharedPreferences(this)
                    .getString(KEY_DEFAULT_SERVER_IP_ADDRESS, "")
        }
    }

    override fun onJoystickMoved(region: Int, id: Int) {
        if (id == R.id.leftJoystickView) {
            when (region) {
                1 -> leftJoystickCommand = "65"
                2 -> leftJoystickCommand = "67"
                3 -> leftJoystickCommand = "66"
                4 -> leftJoystickCommand = "68"
                else -> {
                    leftJoystickCommand = "00"
                }
            }
        } else if (id == R.id.rightJoystickView) {
            when (region) {
                1 -> rightJoystickCommand = "119"
                2 -> rightJoystickCommand = "100"
                3 -> rightJoystickCommand = "120"
                4 -> rightJoystickCommand = "97"
                else -> {
                    rightJoystickCommand = "00"
                }
            }
        }
    }

    fun onConnectButtonClick(view: View?) {
        if (this.serverIPAddress != null && view is Button) {
            view.text = resources.getString(R.string.connect_yes)
            view.isEnabled = false
            Log.d("PREFERENCES", "ATTEMPTING CONNECTIION USING " + serverIPAddress)
            startServerExecutorServices(this.serverIPAddress!!)
        }
    }

    fun onResetButtonClick(view: View?) {
        if (view != null && view is Button) {
            resetCommand = true
        }
    }

    fun onEndButtonClick(view: View?) {
        if (view != null && view is Button) {
            endCommand = true
        }
    }

    fun onArmButtonClick(view: View?) {
        if (view != null && view is Button) {
            armCommand = true
        }
    }

    fun onDisarmButtonCLick(view: View?) {
        if (view != null && view is Button) {
            disarmCommand = true
        }
    }

    fun startServerExecutorServices(address: String) {

        val looper = Looper.getMainLooper()
        val runnable: Runnable = object : Runnable {
            override fun run() {
                try {
                    val serverSocket = Socket(address, 8888)
                    val socketWriter = PrintWriter(serverSocket.getOutputStream(), true)
                    val socketReader = BufferedReader(InputStreamReader(serverSocket.getInputStream()))

                    // If we reached here without raising an exception,
                    // out connection successful
                    Handler(looper).post { showToast("Connected to " + address) }
                    Log.d("JOYSTICKSERVER", "CONNECTED")

                    val rightRunnable: Runnable = object : Runnable {
                        override fun run() {
                            if (rightJoystickCommand == "00" && leftJoystickCommand == "00") {
                                socketWriter.println("00")
                            } else {
                                if (rightJoystickCommand != "00") {
                                    socketWriter.println(rightJoystickCommand)
                                }
                                if (leftJoystickCommand != "00") {
                                    socketWriter.println(leftJoystickCommand)
                                }
                            }
                            if (endCommand != false) {
                                socketWriter.println("63")
                                endCommand = false
                            }
                            if (resetCommand != false) {
                                socketWriter.println("115")
                                resetCommand = false
                            }
                            if (armCommand != false) {
                                socketWriter.println("107")
                                armCommand = false
                            }
                            if (disarmCommand != false) {
                                socketWriter.println("108")
                                disarmCommand = false
                            }
                        }
                    }

                    val leftRunnable: Runnable = object : Runnable {
                        override fun run() {
                            while(true) {
                                Log.d("JOYSTICKSERVER", socketReader.readLine())
                            }
                        }
                    }

                    var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
                    scheduledExecutorService.scheduleAtFixedRate(rightRunnable, 0, 100, TimeUnit.MILLISECONDS)

                    var executorService = Executors.newSingleThreadExecutor()
                    executorService.execute(leftRunnable)

                } catch (e: Exception) {
                    Log.d("JOYSTICKSERVER", e.toString())
                    Handler(looper).post {
                        showToast(e.toString())
                        updateConnectButton(true)
                    }
                }
            }
        }
        var executorService = Executors.newSingleThreadExecutor()
        executorService.execute(runnable)
    }

    fun showToast(s: String) {
        Log.d("TOAS", "LAUNCHED")
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

    fun updateConnectButton(isEnabled: Boolean) {

        connectButton.text = if (isEnabled == false) resources.getString(R.string.connect_yes)
                                else resources.getString(R.string.connect)
        connectButton.isEnabled = isEnabled
    }
}
