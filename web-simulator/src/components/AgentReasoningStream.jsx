import React from 'react';
import { 
  Cpu, 
  CheckCircle2, 
  AlertTriangle, 
  ShieldAlert, 
  Layers, 
  Terminal, 
  Volume2, 
  FastForward, 
  Pause, 
  Play, 
  RotateCcw 
} from 'lucide-react';

export default function AgentReasoningStream({
  steps,
  currentStepIndex,
  agentState,
  goal,
  executionSpeed,
  setExecutionSpeed,
  isPaused,
  setIsPaused,
  onReset,
  confirmationRequired,
  onAuthorizeConfirmation
}) {
  return (
    <div className="bg-agent-card rounded-2xl p-5 border border-agent-border shadow-xl space-y-4">
      {/* Header & Controls */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 pb-3">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
            <Cpu className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-bold text-sm text-white">Agent Reasoning & Execution Feed</h3>
            <p className="text-xs text-slate-400">Live ReAct (Reasoning + Acting) loop</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          {/* Speed selector */}
          <div className="flex bg-slate-900 rounded-lg p-0.5 border border-slate-800 text-[11px]">
            {[
              { label: '0.5x', val: 2000 },
              { label: '1x', val: 1000 },
              { label: '2x', val: 500 }
            ].map((s) => (
              <button
                key={s.label}
                onClick={() => setExecutionSpeed(s.val)}
                className={`px-2 py-0.5 rounded-md transition-colors ${
                  executionSpeed === s.val ? 'bg-indigo-600 text-white font-bold' : 'text-slate-400 hover:text-white'
                }`}
              >
                {s.label}
              </button>
            ))}
          </div>

          {/* Pause / Play */}
          <button
            onClick={() => setIsPaused(!isPaused)}
            className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg border border-slate-700"
            title={isPaused ? "Resume" : "Pause"}
          >
            {isPaused ? <Play className="w-3.5 h-3.5 fill-current" /> : <Pause className="w-3.5 h-3.5" />}
          </button>

          {/* Reset */}
          <button
            onClick={onReset}
            className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg border border-slate-700"
            title="Reset"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Goal Display */}
      {goal && (
        <div className="bg-slate-900/80 p-3 rounded-xl border border-indigo-500/30 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Terminal className="w-4 h-4 text-indigo-400" />
            <span className="text-xs font-semibold text-indigo-300">Target Goal:</span>
            <span className="text-xs text-white font-medium">"{goal}"</span>
          </div>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 font-bold">
            {currentStepIndex + 1} / {steps.length || 1} STEPS
          </span>
        </div>
      )}

      {/* Safety Guardrail Confirmation Modal */}
      {confirmationRequired && (
        <div className="bg-amber-500/10 border-2 border-amber-500/60 rounded-xl p-4 space-y-3 animate-pulse-subtle">
          <div className="flex items-center space-x-2 text-amber-400 font-bold text-xs">
            <ShieldAlert className="w-5 h-5 text-amber-400 shrink-0" />
            <span>SENSITIVE TRANSACTION SAFETY INTERCEPT</span>
          </div>
          <p className="text-xs text-slate-200">
            OmniAgent is requesting explicit user confirmation before authorizing payment or booking:
          </p>
          <div className="p-2.5 bg-slate-900 rounded-lg text-xs font-mono text-amber-300 border border-amber-500/30">
            {confirmationRequired.message}
          </div>
          <div className="flex space-x-3 pt-1">
            <button
              onClick={() => onAuthorizeConfirmation(true)}
              className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-lg shadow-md"
            >
              ✓ Authorize Action
            </button>
            <button
              onClick={() => onAuthorizeConfirmation(false)}
              className="flex-1 py-2 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs rounded-lg shadow-md"
            >
              ✕ Decline
            </button>
          </div>
        </div>
      )}

      {/* Steps List */}
      <div className="space-y-2.5 max-h-[350px] overflow-y-auto pr-1">
        {steps.length === 0 ? (
          <div className="text-center py-8 text-slate-500 text-xs">
            No active task. Choose a voice command prompt above to begin.
          </div>
        ) : (
          steps.map((st, index) => {
            const isCurrent = index === currentStepIndex;
            const isCompleted = index < currentStepIndex;
            const isPending = index > currentStepIndex;

            return (
              <div
                key={st.step}
                className={`p-3 rounded-xl border transition-all ${
                  isCurrent
                    ? 'bg-indigo-950/40 border-indigo-500/80 shadow-md ring-1 ring-indigo-500/30'
                    : isCompleted
                    ? 'bg-slate-900/60 border-slate-800 opacity-80'
                    : 'bg-slate-900/30 border-slate-800/50 opacity-40'
                }`}
              >
                <div className="flex items-center justify-between mb-1.5">
                  <div className="flex items-center space-x-2">
                    <span className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold ${
                      isCompleted ? 'bg-emerald-500 text-white' : isCurrent ? 'bg-indigo-600 text-white animate-pulse' : 'bg-slate-800 text-slate-400'
                    }`}>
                      {isCompleted ? '✓' : st.step}
                    </span>
                    <span className="text-xs font-bold text-slate-200">
                      Step {st.step}: {st.targetName}
                    </span>
                  </div>

                  <span className={`text-[10px] uppercase font-mono px-2 py-0.5 rounded font-bold ${
                    st.action === 'click' ? 'bg-blue-500/20 text-blue-400' :
                    st.action === 'type' ? 'bg-emerald-500/20 text-emerald-400' :
                    st.action === 'launch_app' ? 'bg-purple-500/20 text-purple-400' :
                    st.action === 'ask_confirmation' ? 'bg-amber-500/20 text-amber-400' :
                    'bg-emerald-600/30 text-emerald-300'
                  }`}>
                    {st.action}
                  </span>
                </div>

                <p className="text-xs text-slate-300 pl-7 leading-relaxed font-sans">
                  {st.thought}
                </p>

                {st.coords && isCurrent && (
                  <div className="pl-7 mt-2 flex items-center space-x-3 text-[11px] font-mono text-slate-400">
                    <span>Node ID: <strong className="text-indigo-300">[{st.targetId}]</strong></span>
                    <span>Coordinates: ({st.coords.x}, {st.coords.y})</span>
                  </div>
                )}

                {st.speech && (
                  <div className="pl-7 mt-1.5 flex items-center space-x-1.5 text-[11px] text-emerald-400">
                    <Volume2 className="w-3 h-3 shrink-0" />
                    <span className="italic">"{st.speech}"</span>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
