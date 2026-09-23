import React from 'react';
import { X, Key, ShieldCheck, Cpu } from 'lucide-react';

export default function ApiKeyModal({
  isOpen,
  onClose,
  provider,
  setProvider,
  apiKey,
  setApiKey,
  model,
  setModel
}) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div className="bg-agent-card border border-agent-border w-full max-w-md rounded-2xl p-6 shadow-2xl space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center space-x-2">
            <Key className="w-5 h-5 text-indigo-400" />
            <h3 className="font-bold text-sm text-white">LLM Reasoning Engine Configuration</h3>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-3 text-xs">
          <div>
            <label className="text-slate-300 font-semibold block mb-1">Provider</label>
            <div className="grid grid-cols-3 gap-2">
              {['OPENAI', 'ANTHROPIC', 'GEMINI'].map((p) => (
                <button
                  key={p}
                  onClick={() => {
                    setProvider(p);
                    if (p === 'OPENAI') setModel('gpt-4o');
                    if (p === 'ANTHROPIC') setModel('claude-3-5-sonnet-20241022');
                    if (p === 'GEMINI') setModel('gemini-2.0-flash');
                  }}
                  className={`py-2 rounded-xl border text-xs font-semibold ${
                    provider === p
                      ? 'bg-indigo-600 border-indigo-500 text-white shadow-md'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  {p}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="text-slate-300 font-semibold block mb-1">Model Name</label>
            <input
              type="text"
              value={model}
              onChange={(e) => setModel(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="text-slate-300 font-semibold block mb-1">
              API Key (Optional)
            </label>
            <input
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="Leave blank to use built-in intelligent simulator"
              className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-xl flex items-start space-x-2 text-emerald-300">
            <ShieldCheck className="w-4 h-4 shrink-0 mt-0.5" />
            <div className="text-[11px] leading-relaxed">
              <strong>Simulator Ready:</strong> If no API key is provided, OmniAgent operates with full realistic multi-step planning heuristics for messaging, food delivery, rides, alarms, and settings.
            </div>
          </div>
        </div>

        <button
          onClick={onClose}
          className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs rounded-xl shadow-lg"
        >
          Save & Continue
        </button>
      </div>
    </div>
  );
}
