import React, { useState } from 'react';
import { Network, Eye, Code, Copy, Check } from 'lucide-react';

export default function AccessibilityTreeViewer({ currentApp, showInspector, setShowInspector }) {
  const [viewMode, setViewMode] = useState('table'); // 'table' or 'raw'
  const [copied, setCopied] = useState(false);

  // Generate simulated accessibility tree for the active app
  const nodes = getNodesForApp(currentApp);

  const rawPromptText = `[ACCESSIBILITY HIERARCHY DUMP]
App Package: ${nodes.packageName}
Interactive Screen Elements:
${nodes.items.map(n => `[${n.id}] ${n.className} ${n.text ? `"${n.text}"` : `desc="${n.desc}"`} bounds=(${n.bounds}) [${[n.clickable && 'clickable', n.editable && 'editable'].filter(Boolean).join(', ')}]`).join('\n')}`;

  const copyPrompt = () => {
    navigator.clipboard.writeText(rawPromptText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="bg-agent-card rounded-2xl p-5 border border-agent-border shadow-xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 pb-3">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
            <Network className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-bold text-sm text-white">Android Accessibility Inspector</h3>
            <p className="text-xs text-slate-400">Live `AccessibilityNodeInfo` tree parsed for LLM</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowInspector(!showInspector)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-all ${
              showInspector 
                ? 'bg-rose-500/20 text-rose-300 border border-rose-500/50' 
                : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700'
            }`}
          >
            <Eye className="w-3.5 h-3.5" />
            <span>{showInspector ? 'Hide [ID] Badges' : 'Show [ID] Badges'}</span>
          </button>

          <div className="flex bg-slate-900 rounded-lg p-0.5 border border-slate-800 text-xs">
            <button
              onClick={() => setViewMode('table')}
              className={`px-2.5 py-1 rounded-md ${viewMode === 'table' ? 'bg-indigo-600 text-white font-semibold' : 'text-slate-400'}`}
            >
              Table
            </button>
            <button
              onClick={() => setViewMode('raw')}
              className={`px-2.5 py-1 rounded-md ${viewMode === 'raw' ? 'bg-indigo-600 text-white font-semibold' : 'text-slate-400'}`}
            >
              LLM Prompt
            </button>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between text-xs text-slate-400">
        <span>Active Package: <strong className="text-indigo-400 font-mono">{nodes.packageName}</strong></span>
        <span>{nodes.items.length} Interactive Elements Found</span>
      </div>

      {viewMode === 'table' ? (
        <div className="overflow-x-auto max-h-[300px] overflow-y-auto border border-slate-800 rounded-xl">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-900/80 text-slate-400 font-mono border-b border-slate-800">
              <tr>
                <th className="p-2.5">ID</th>
                <th className="p-2.5">Class / Element</th>
                <th className="p-2.5">Text / ContentDesc</th>
                <th className="p-2.5">Bounds</th>
                <th className="p-2.5">Traits</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 font-mono">
              {nodes.items.map((node) => (
                <tr key={node.id} className="hover:bg-slate-800/40">
                  <td className="p-2.5 text-rose-400 font-bold">[{node.id}]</td>
                  <td className="p-2.5 text-slate-300">{node.className.split('.').pop()}</td>
                  <td className="p-2.5 text-emerald-300 font-sans">{node.text || node.desc || '-'}</td>
                  <td className="p-2.5 text-slate-400 text-[11px]">{node.bounds}</td>
                  <td className="p-2.5">
                    <span className="px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-300 text-[10px]">
                      {node.clickable ? 'clickable' : ''} {node.editable ? 'editable' : ''}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="relative">
          <button
            onClick={copyPrompt}
            className="absolute top-2.5 right-2.5 px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-md border border-slate-700 flex items-center space-x-1"
          >
            {copied ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
            <span>{copied ? 'Copied' : 'Copy'}</span>
          </button>
          <pre className="bg-slate-950 p-4 rounded-xl text-xs font-mono text-slate-300 max-h-[300px] overflow-y-auto leading-relaxed border border-slate-800">
            {rawPromptText}
          </pre>
        </div>
      )}
    </div>
  );
}

function getNodesForApp(app) {
  switch (app) {
    case 'home':
      return {
        packageName: 'com.google.android.apps.nexuslauncher',
        items: [
          { id: 101, className: 'android.widget.TextView', text: 'WhatsApp', bounds: '40,280,120,360', clickable: true },
          { id: 102, className: 'android.widget.TextView', text: 'DoorDash', bounds: '150,280,230,360', clickable: true },
          { id: 103, className: 'android.widget.TextView', text: 'Uber', bounds: '260,280,340,360', clickable: true },
          { id: 104, className: 'android.widget.TextView', text: 'Settings', bounds: '40,480,120,560', clickable: true },
          { id: 105, className: 'android.widget.TextView', text: 'Clock', bounds: '40,380,120,460', clickable: true },
          { id: 106, className: 'android.widget.TextView', text: 'Camera', bounds: '150,380,230,460', clickable: true }
        ]
      };
    case 'whatsapp_list':
      return {
        packageName: 'com.whatsapp',
        items: [
          { id: 201, className: 'android.widget.ImageView', desc: 'Search', bounds: '300,40,330,70', clickable: true },
          { id: 202, className: 'android.view.ViewGroup', text: 'Alex Rivera: Are you almost here?', bounds: '10,90,330,170', clickable: true },
          { id: 203, className: 'android.view.ViewGroup', text: 'Mom: Call me when you are free', bounds: '10,180,330,260', clickable: true }
        ]
      };
    case 'whatsapp_chat':
      return {
        packageName: 'com.whatsapp',
        items: [
          { id: 204, className: 'android.widget.ImageButton', desc: 'Navigate up', bounds: '10,40,40,70', clickable: true },
          { id: 205, className: 'android.widget.EditText', text: 'Type a message...', bounds: '20,680,290,730', clickable: true, editable: true },
          { id: 206, className: 'android.widget.FloatingActionButton', desc: 'Send', bounds: '300,680,340,730', clickable: true }
        ]
      };
    case 'doordash_home':
    case 'doordash_restaurant':
    case 'doordash_cart':
    case 'doordash_checkout':
    case 'doordash_success':
      return {
        packageName: 'com.doordash.android',
        items: [
          { id: 301, className: 'android.widget.TextView', text: 'Home (25-35m)', bounds: '20,40,200,70', clickable: true },
          { id: 302, className: 'android.view.ViewGroup', text: "Luigi's Woodfire Pizza - 4.9 stars", bounds: '20,120,320,240', clickable: true },
          { id: 304, className: 'android.widget.Button', text: 'Add Margherita Pizza ($18.50)', bounds: '250,220,320,260', clickable: true },
          { id: 308, className: 'android.widget.Button', text: 'Place Order ($19.49)', bounds: '20,660,320,710', clickable: true }
        ]
      };
    case 'uber_home':
    case 'uber_rides':
    case 'uber_confirmed':
      return {
        packageName: 'com.ubercab',
        items: [
          { id: 401, className: 'android.widget.TextView', text: 'Where are you heading?', bounds: '20,100,280,140', clickable: false },
          { id: 402, className: 'android.widget.EditText', text: 'Where to?', bounds: '20,180,320,230', clickable: true, editable: true },
          { id: 405, className: 'android.view.ViewGroup', text: 'Uber Comfort - $31.20', bounds: '20,460,320,530', clickable: true },
          { id: 408, className: 'android.widget.Button', text: 'Confirm Comfort ($31.20)', bounds: '20,660,320,710', clickable: true }
        ]
      };
    case 'clock_list':
    case 'clock_add':
      return {
        packageName: 'com.google.android.deskclock',
        items: [
          { id: 501, className: 'android.widget.Switch', text: '7:00 AM Alarm Toggle', bounds: '260,110,310,140', clickable: true },
          { id: 502, className: 'android.widget.ImageButton', desc: 'Add alarm', bounds: '140,650,200,710', clickable: true }
        ]
      };
    default:
      return {
        packageName: 'com.android.settings',
        items: [
          { id: 601, className: 'android.widget.TextView', text: 'Settings', bounds: '20,40,150,70', clickable: false },
          { id: 603, className: 'android.view.ViewGroup', text: 'Display & Brightness', bounds: '20,110,320,160', clickable: true },
          { id: 605, className: 'android.widget.Switch', text: 'Dark Theme Toggle', bounds: '260,180,310,210', clickable: true }
        ]
      };
  }
}
