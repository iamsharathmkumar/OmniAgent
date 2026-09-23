import React from 'react';
import { BookOpen, Shield, Zap, Terminal, Smartphone, Layers, Lock, Cpu } from 'lucide-react';

export default function DocsHub() {
  return (
    <div className="bg-agent-card rounded-2xl p-6 border border-agent-border shadow-xl space-y-6 text-slate-300">
      <div className="flex items-center space-x-3 border-b border-slate-800 pb-4">
        <div className="w-10 h-10 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
          <BookOpen className="w-5 h-5" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-white">OmniAgent Android: System Architecture & Developer Guide</h2>
          <p className="text-xs text-slate-400">Technical blueprint for autonomous mobile operating system agents</p>
        </div>
      </div>

      {/* Grid of key concepts */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-slate-900/70 p-4 rounded-xl border border-slate-800 space-y-2">
          <div className="flex items-center space-x-2 text-indigo-400 font-bold text-sm">
            <Layers className="w-4 h-4" />
            <span>1. UI Inspection</span>
          </div>
          <p className="text-xs text-slate-400 leading-relaxed">
            Reads Android's live <code className="text-indigo-300">AccessibilityNodeInfo</code> tree to locate elements, buttons, text fields, and descriptions without requiring root access.
          </p>
        </div>

        <div className="bg-slate-900/70 p-4 rounded-xl border border-slate-800 space-y-2">
          <div className="flex items-center space-x-2 text-emerald-400 font-bold text-sm">
            <Zap className="w-4 h-4" />
            <span>2. Synthetic Actions</span>
          </div>
          <p className="text-xs text-slate-400 leading-relaxed">
            Executes taps, swipes, and pinches via <code className="text-emerald-300">dispatchGesture()</code>, inputs text via <code className="text-emerald-300">ACTION_SET_TEXT</code>, and navigates with <code className="text-emerald-300">performGlobalAction()</code>.
          </p>
        </div>

        <div className="bg-slate-900/70 p-4 rounded-xl border border-slate-800 space-y-2">
          <div className="flex items-center space-x-2 text-amber-400 font-bold text-sm">
            <Shield className="w-4 h-4" />
            <span>3. Safety Guardrails</span>
          </div>
          <p className="text-xs text-slate-400 leading-relaxed">
            Intercepts financial transactions, checkout buttons, and account deletion with explicit user confirmation modals and an instant emergency halt button.
          </p>
        </div>
      </div>

      {/* Deep-dive sections */}
      <div className="space-y-4">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2">
          <Cpu className="w-4 h-4 text-indigo-400" />
          <span>Core Agent Loop (The Autonomous Cycle)</span>
        </h3>
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-xs font-mono space-y-2 leading-relaxed">
          <div className="text-indigo-300 font-semibold">[1] Voice Capture:</div>
          <p className="text-slate-400 pl-4">Continuous Foreground Service with SpeechRecognizer converts microphone audio stream into user intent (e.g., "Order a Margherita pizza").</p>
          
          <div className="text-indigo-300 font-semibold">[2] Screen Grounding:</div>
          <p className="text-slate-400 pl-4">UIHierarchyParser traverses active window nodes, assigns numeric element IDs [1], [2], filters out invisible views, and computes touch centroids.</p>

          <div className="text-indigo-300 font-semibold">[3] Multimodal Reasoning:</div>
          <p className="text-slate-400 pl-4">LLM (GPT-4o / Claude 3.5 Sonnet / Gemini 2.0 Flash) receives current goal, action history, and UI elements, outputting the next single executable action in strict JSON.</p>

          <div className="text-indigo-300 font-semibold">[4] OS Execution:</div>
          <p className="text-slate-400 pl-4">AgentAccessibilityService dispatches synthetic clicks or gestures. If a sensitive action is detected, it triggers the Floating HUD confirmation dialog.</p>

          <div className="text-indigo-300 font-semibold">[5] Speech Feedback:</div>
          <p className="text-slate-400 pl-4">Android TextToSpeech speaks progress and task completion back to the user ("Sent WhatsApp message to Alex").</p>
        </div>
      </div>

      {/* Permissions Breakdown */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2">
          <Lock className="w-4 h-4 text-emerald-400" />
          <span>Required Android Permissions</span>
        </h3>
        <div className="overflow-x-auto border border-slate-800 rounded-xl">
          <table className="w-full text-xs text-left">
            <thead className="bg-slate-900 text-slate-400 font-mono">
              <tr>
                <th className="p-3">Permission</th>
                <th className="p-3">Why OmniAgent Requires It</th>
                <th className="p-3">Activation Method</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              <tr>
                <td className="p-3 font-mono text-indigo-400">BIND_ACCESSIBILITY_SERVICE</td>
                <td className="p-3">Allows inspecting UI hierarchy & executing synthetic touch gestures</td>
                <td className="p-3">Settings → Accessibility → OmniAgent (ON)</td>
              </tr>
              <tr>
                <td className="p-3 font-mono text-indigo-400">SYSTEM_ALERT_WINDOW</td>
                <td className="p-3">Renders draggable floating mic HUD and thought status over other apps</td>
                <td className="p-3">Settings → Special App Access → Display Over Other Apps</td>
              </tr>
              <tr>
                <td className="p-3 font-mono text-indigo-400">RECORD_AUDIO</td>
                <td className="p-3">Captures voice commands through device microphone</td>
                <td className="p-3">Runtime Permission Prompt</td>
              </tr>
              <tr>
                <td className="p-3 font-mono text-indigo-400">FOREGROUND_SERVICE</td>
                <td className="p-3">Prevents Android OS from killing the voice assistant service in background</td>
                <td className="p-3">AndroidManifest.xml Declaration</td>
              </tr>
              <tr>
                <td className="p-3 font-mono text-indigo-400">QUERY_ALL_PACKAGES</td>
                <td className="p-3">Allows launching any installed app requested by voice</td>
                <td className="p-3">AndroidManifest.xml Declaration</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* How to deploy */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2">
          <Terminal className="w-4 h-4 text-amber-400" />
          <span>How to Build & Deploy to Device</span>
        </h3>
        <ol className="list-decimal list-inside space-y-2 text-xs text-slate-300">
          <li>Download the project via the <strong>Download Android Project (.zip)</strong> button or open <code className="text-indigo-300">/home/user/OmniAgent-Android</code>.</li>
          <li>Open the folder in <strong>Android Studio</strong> (Jellyfish, Koala or newer).</li>
          <li>Connect your physical Android smartphone with <strong>USB Debugging</strong> enabled.</li>
          <li>Run the provided script <code className="text-emerald-300">./setup-and-grant-permissions.sh</code> to automatically configure all permissions via ADB.</li>
          <li>Press <strong>Run 'app'</strong> in Android Studio. OmniAgent will start and present the floating Dynamic Island HUD!</li>
        </ol>
      </div>
    </div>
  );
}
