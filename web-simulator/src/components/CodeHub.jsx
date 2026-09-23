import React, { useState } from 'react';
import { FileCode2, Copy, Check, Terminal, Download, ExternalLink, ShieldCheck } from 'lucide-react';
import { CODE_FILES } from '../data/codeSamples';

export default function CodeHub() {
  const [selectedFile, setSelectedFile] = useState(CODE_FILES[0]);
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedAdb, setCopiedAdb] = useState(false);

  const adbCommands = `# Grant Android OS Permissions to OmniAgent
adb shell pm grant com.omniagent.assistant android.permission.RECORD_AUDIO
adb shell appops set com.omniagent.assistant SYSTEM_ALERT_WINDOW allow
adb shell settings put secure enabled_accessibility_services com.omniagent.assistant/com.omniagent.assistant.service.AgentAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell dumpsys deviceidle whitelist +com.omniagent.assistant
adb shell am start -n com.omniagent.assistant/.ui.MainActivity`;

  const copyCode = () => {
    navigator.clipboard.writeText(selectedFile.code);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const copyAdb = () => {
    navigator.clipboard.writeText(adbCommands);
    setCopiedAdb(true);
    setTimeout(() => setCopiedAdb(false), 2000);
  };

  const handleDownloadSource = () => {
    const link = document.createElement('a');
    link.href = '/OmniAgent-Android.zip';
    link.download = 'OmniAgent-Android.zip';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="bg-agent-card rounded-2xl p-5 border border-agent-border shadow-xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 pb-3">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
            <FileCode2 className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-bold text-sm text-white">Android Source Code & Setup Hub</h3>
            <p className="text-xs text-slate-400">Native Kotlin Accessibility Engine, Manifest & Services</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => {
              const link = document.createElement('a');
              link.href = '/OmniAgent-v1.0.apk';
              link.download = 'OmniAgent-v1.0.apk';
              document.body.appendChild(link);
              link.click();
              document.body.removeChild(link);
            }}
            className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs rounded-xl flex items-center space-x-1.5 shadow-md transition-all ring-2 ring-indigo-400/50"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download .APK (Ready to Install)</span>
          </button>

          <button
            onClick={handleDownloadSource}
            className="px-3.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs rounded-xl flex items-center space-x-1.5 border border-slate-700 shadow-sm transition-all"
          >
            <Download className="w-3.5 h-3.5 text-emerald-400" />
            <span>Source Code (.zip)</span>
          </button>
        </div>
      </div>

      {/* File selector tabs */}
      <div className="flex flex-wrap gap-1.5">
        {CODE_FILES.map((f) => (
          <button
            key={f.path}
            onClick={() => setSelectedFile(f)}
            className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-colors ${
              selectedFile.path === f.path
                ? 'bg-indigo-600 text-white font-bold shadow-md'
                : 'bg-slate-800/80 text-slate-300 hover:bg-slate-700/80 border border-slate-700/50'
            }`}
          >
            {f.name}
          </button>
        ))}
      </div>

      {/* File Description */}
      <div className="text-xs text-indigo-300 bg-indigo-950/30 p-2.5 rounded-xl border border-indigo-900/40">
        <strong className="text-white font-mono">{selectedFile.path}:</strong> {selectedFile.description}
      </div>

      {/* Code Editor Preview */}
      <div className="relative">
        <button
          onClick={copyCode}
          className="absolute top-3 right-3 px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-lg border border-slate-700 flex items-center space-x-1 shadow-md z-10"
        >
          {copiedCode ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
          <span>{copiedCode ? 'Copied' : 'Copy File'}</span>
        </button>
        <pre className="bg-slate-950 p-4 rounded-xl text-xs font-mono text-slate-300 max-h-[340px] overflow-y-auto leading-relaxed border border-slate-800">
          <code>{selectedFile.code}</code>
        </pre>
      </div>

      {/* ADB Permission Generator Card */}
      <div className="bg-slate-900/70 p-4 rounded-xl border border-slate-800 space-y-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2 text-xs font-bold text-slate-200">
            <Terminal className="w-4 h-4 text-emerald-400" />
            <span>One-Command ADB Setup (Grant all OS permissions instantly)</span>
          </div>
          <button
            onClick={copyAdb}
            className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-md border border-slate-700 flex items-center space-x-1"
          >
            {copiedAdb ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
            <span>{copiedAdb ? 'Copied' : 'Copy ADB'}</span>
          </button>
        </div>
        <pre className="bg-slate-950 p-3 rounded-lg text-[11px] font-mono text-emerald-400 overflow-x-auto border border-slate-800">
          {adbCommands}
        </pre>
      </div>
    </div>
  );
}
