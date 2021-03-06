/*
 * Copyright 2017 KG Soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kgurgul.cpuinfo.features.information.hardware

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.viewModels
import com.kgurgul.cpuinfo.R
import com.kgurgul.cpuinfo.features.information.base.BaseRvFragment
import com.kgurgul.cpuinfo.features.information.base.InfoItemsAdapter
import com.kgurgul.cpuinfo.utils.DividerItemDecoration
import com.kgurgul.cpuinfo.utils.MIME_TEXT_PLAIN
import com.kgurgul.cpuinfo.utils.createSafFile
import com.kgurgul.cpuinfo.utils.lifecycleawarelist.ListLiveDataObserver
import com.kgurgul.cpuinfo.utils.runOnApiAbove
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment responsible for hardware info. It also contains [BroadcastReceiver] for AC connection.
 *
 * @author kgurgul
 */
@AndroidEntryPoint
class HardwareInfoFragment : BaseRvFragment() {

    private val viewModel: HardwareInfoViewModel by viewModels()

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.refreshHardwareInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        runOnApiAbove(18) {
            inflater.inflate(R.menu.info_menu, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.action_export_to_csv -> {
                    createSafFile(MIME_TEXT_PLAIN, DUMP_FILENAME, RC_CREATE_FILE)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED")
        intentFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED")
        requireActivity().registerReceiver(powerReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(powerReceiver)
    }

    override fun setupRecyclerViewAdapter() {
        val infoItemsAdapter = InfoItemsAdapter(viewModel.listLiveData,
                InfoItemsAdapter.LayoutType.HORIZONTAL_LAYOUT, onClickListener = this)
        viewModel.listLiveData.listStatusChangeNotificator.observe(viewLifecycleOwner,
                ListLiveDataObserver(infoItemsAdapter))
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext()))
        recyclerView.adapter = infoItemsAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_CREATE_FILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.also { uri ->
                        viewModel.saveListToFile(uri)
                    }
                }
            }
        }
    }

    companion object {
        private const val RC_CREATE_FILE = 100
        private const val DUMP_FILENAME = "hardware_info.txt"
    }
}