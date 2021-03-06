package io.bluetrace.opentrace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.database_peek.*
import io.bluetrace.opentrace.streetpass.persistence.StreetPassRecordStorage
import io.bluetrace.opentrace.streetpass.view.RecordViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.safeblues.android.API
import org.safeblues.android.CD
import org.safeblues.android.CDWorker


class PeekActivity : AppCompatActivity() {

    private lateinit var viewModel: RecordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        newPeek()
    }

    private fun syncStrands(view: View) {
        val api = API
        GlobalScope.launch { // or whatever
            api.syncStrandsWithServer(view.context)
            api.pushStatsToServer(view.context)
        }
    }

    private fun setExperimentButtonState(context: Context) {
        val in_experiment = Preference.getInExperiment(context)
        if (in_experiment) {
            sb_experiment_button.text = "Experiment (on: " + Preference.getExperimentId(context) + ")"
        } else {
            sb_experiment_button.text = "Experiment (off)"
        }
    }

    private fun newPeek() {
        setContentView(R.layout.database_peek)
        val adapter = RecordListAdapter(this)
        recyclerview.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        recyclerview.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            recyclerview.context,
            layoutManager.orientation
        )
        recyclerview.addItemDecoration(dividerItemDecoration)

        viewModel = ViewModelProvider(this).get(RecordViewModel::class.java)
        viewModel.allRecords.observe(this, Observer { records ->
            adapter.setSourceData(records)
        })

        expand.setOnClickListener {
            viewModel.allRecords.value?.let {
                adapter.setMode(RecordListAdapter.MODE.ALL)
            }
        }

        collapse.setOnClickListener {
            viewModel.allRecords.value?.let {
                adapter.setMode(RecordListAdapter.MODE.COLLAPSE)
            }
        }

        start.setOnClickListener {
            startService()
        }

        stop.setOnClickListener {
            stopService()
        }

        push_pull.setOnClickListener {
            syncStrands(it)
        }

        val seed_all = Preference.getSeedAll(this)

        if (seed_all) {
            seed_all_on.visibility = View.VISIBLE
            seed_all_off.visibility = View.GONE
        } else {
            seed_all_on.visibility = View.GONE
            seed_all_off.visibility = View.VISIBLE
        }

        seed_all_on.setOnClickListener{
            CD.testSeeeding(it.context)
            Preference.putSeedAll(it.context, false)
            seed_all_on.visibility = View.GONE
            seed_all_off.visibility = View.VISIBLE
        }

        seed_all_off.setOnClickListener{
            Preference.putSeedAll(it.context, true)
            seed_all_on.visibility = View.VISIBLE
            seed_all_off.visibility = View.GONE
        }

        sb_experiment_button.setOnClickListener{
            val in_experiment = Preference.getInExperiment(it.context)
            if (in_experiment) {
                // end of experiment
                CDWorker.enqueueUpdate(it.context)
            } else {
                // start of new one
                Preference.getNextExperimentId(it.context)
            }
            Preference.putInExperiment(it.context, !in_experiment)
            setExperimentButtonState(it.context)
        }

        setExperimentButtonState(this)

        delete.setOnClickListener { view ->
            view.isEnabled = false

            val builder = AlertDialog.Builder(this)
            builder
                .setTitle("Are you sure?")
                .setCancelable(false)
                .setMessage("Deleting the DB records is irreversible")
                .setPositiveButton("DELETE") { dialog, which ->
                    Observable.create<Boolean> {
                        StreetPassRecordStorage(this).nukeDb()
                        it.onNext(true)
                    }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe { result ->
                            Toast.makeText(this, "Database nuked: $result", Toast.LENGTH_SHORT)
                                .show()
                            view.isEnabled = true
                            dialog.cancel()
                        }
                }

                .setNegativeButton("DON'T DELETE") { dialog, which ->
                    view.isEnabled = true
                    dialog.cancel()
                }

            val dialog: AlertDialog = builder.create()
            dialog.show()

        }

        plot.setOnClickListener { view ->
            val intent = Intent(this, PlotActivity::class.java)
            intent.putExtra("time_period", nextTimePeriod())
            startActivity(intent)
        }

        val serviceUUID = BuildConfig.BLE_SSID
        info.text =
            "SSID: ${serviceUUID.substring(serviceUUID.length - 4)}.\nAddr: ${android.provider.Settings.Secure.getString(applicationContext.getContentResolver(), "bluetooth_address")}.\nTempID: ${API.getCurrentTempID(applicationContext)}"

        if (!BuildConfig.DEBUG) {
            start.visibility = View.GONE
            stop.visibility = View.GONE
            delete.visibility = View.GONE
        }
    }

    private var timePeriod: Int = 0

    private fun nextTimePeriod(): Int {
        timePeriod = when (timePeriod) {
            1 -> 3
            3 -> 6
            6 -> 12
            12 -> 24
            else -> 1
        }

        return timePeriod
    }


    private fun startService() {
        Utils.startBluetoothMonitoringService(this)
    }

    private fun stopService() {
        Utils.stopBluetoothMonitoringService(this)
    }

}
