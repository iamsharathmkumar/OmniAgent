import React, { useState, useEffect, useRef } from 'react';
import confetti from 'canvas-confetti';
import { 
  Smartphone, 
  Cpu, 
  FileCode, 
  Settings2, 
  ShieldCheck, 
  Sparkles, 
  Layers, 
  Activity, 
  Mic, 
  Volume2, 
  Zap, 
  HelpCircle,
  ExternalLink,
  Bot
} from 'lucide-react';

import PhoneMockup from './components/PhoneMockup';
import VoiceController from './components/VoiceController';
import AgentReasoningStream from './components/AgentReasoningStream';
import AccessibilityTreeViewer from './components/AccessibilityTreeViewer';
import CodeHub from './components/CodeHub';
import DocsHub from './components/DocsHub';
import ApiKeyModal from './components/ApiKeyModal';
import { SAMPLE_COMMANDS } from './data/mockApps';

export default function App() {
  const [activeTab, setActiveTab] = useState('phone'); // 'phone' | 'inspector' | 'code' | 'docs'
  const [currentApp, setCurrentApp] = useState('home');
  const [isDarkMode, setIsDarkMode] = useState(true);
  
  // Agent loop states
  const [agentState, setAgentState] = useState('IDLE'); // IDLE, LISTENING, THINKING, EXECUTING, AWAITING_CONFIRMATION, FINISHED
  const [currentThought, setCurrentThought] = useState('');
  const [currentGoal, setCurrentGoal] = useState('');
  const [steps, setSteps] = useState([]);
  const [currentStepIndex, setCurrentStepIndex] = useState(-1);
  const [activeTap, setActiveTap] = useState(null);
  const [typedText, setTypedText] = useState('');
  const [lastSpokenText, setLastSpokenText] = useState('');
  const [showInspector, setShowInspector] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const [executionSpeed, setExecutionSpeed] = useState(1000);
  const [isPaused, setIsPaused] = useState(false);
  const [confirmationRequired, setConfirmationRequired] = useState(null);

  // Settings
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [provider, setProvider] = useState('OPENAI');
  const [apiKey, setApiKey] = useState('');
  const [model, setModel] = useState('gpt-4o');

  const executionTimerRef = useRef(null);
  const pausedRef = useRef(isPaused);
  pausedRef.current = isPaused;

  const speakText = (text) => {
    setLastSpokenText(text);
    if ('speechSynthesis' in window) {
      try {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 1.05;
        utterance.pitch = 1.0;
        window.speechSynthesis.speak(utterance);
      } catch (err) {
        console.warn('Speech synthesis error:', err);
      }
    }
  };

  const executeCommand = (commandObjOrString) => {
    // Clear any running timers
    if (executionTimerRef.current) clearTimeout(executionTimerRef.current);
    setActiveTap(null);
    setTypedText('');
    setConfirmationRequired(null);

    let commandData;
    if (typeof commandObjOrString === 'string') {
      const found = SAMPLE_COMMANDS.find(c => 
        commandObjOrString.toLowerCase().includes(c.id) || 
        c.command.toLowerCase().includes(commandObjOrString.toLowerCase())
      );
      if (found) {
        commandData = found;
      } else {
        // Fallback custom command matching
        commandData = {
          id: 'custom',
          title: 'Custom Command',
          command: commandObjOrString,
          steps: [
            {
              step: 1,
              app: currentApp,
              targetApp: currentApp,
              thought: `Analyzing screen state for intent: "${commandObjOrString}". Target node identified.`,
              action: "click",
              targetId: 101,
              targetName: "Active Window Element",
              coords: { x: 170, y: 350 },
              speech: "Executing requested action on Android."
            },
            {
              step: 2,
              app: currentApp,
              targetApp: currentApp,
              thought: `Completed user request: "${commandObjOrString}".`,
              action: "finish",
              targetName: "Goal Completed",
              speech: `Task finished: "${commandObjOrString}".`
            }
          ]
        };
      }
    } else {
      commandData = commandObjOrString;
    }

    setCurrentGoal(commandData.command);
    setSteps(commandData.steps);
    setCurrentStepIndex(0);
    setAgentState('THINKING');
    setCurrentThought(`Analyzing voice intent: "${commandData.command}"`);
    speakText("On it.");

    runStep(commandData.steps, 0);
  };

  const runStep = (allSteps, index) => {
    if (index >= allSteps.length) {
      setAgentState('FINISHED');
      setCurrentThought('Task completed successfully!');
      confetti({ particleCount: 50, spread: 60, origin: { y: 0.7 } });
      return;
    }

    const current = allSteps[index];
    setCurrentStepIndex(index);
    setAgentState('THINKING');
    setCurrentThought(current.thought);

    executionTimerRef.current = setTimeout(() => {
      if (pausedRef.current) {
        // Wait and check again
        const checkInterval = setInterval(() => {
          if (!pausedRef.current) {
            clearInterval(checkInterval);
            processAction(allSteps, index, current);
          }
        }, 300);
        return;
      }
      processAction(allSteps, index, current);
    }, executionSpeed);
  };

  const processAction = (allSteps, index, current) => {
    if (current.action === 'ask_confirmation') {
      setAgentState('AWAITING_CONFIRMATION');
      setConfirmationRequired({
        message: current.thought,
        onConfirm: () => {
          setConfirmationRequired(null);
          runStep(allSteps, index + 1);
        }
      });
      speakText(current.speech || "Confirmation required.");
      return;
    }

    setAgentState('EXECUTING');
    if (current.coords) {
      setActiveTap(current.coords);
    }

    if (current.speech) {
      speakText(current.speech);
    }

    // Type text animation
    if (current.action === 'type' && current.textValue) {
      let charIdx = 0;
      const typeInterval = setInterval(() => {
        charIdx++;
        setTypedText(current.textValue.substring(0, charIdx));
        if (charIdx >= current.textValue.length) {
          clearInterval(typeInterval);
        }
      }, 50);
    }

    // Delay to let visual tap ripple show, then transition app state
    setTimeout(() => {
      setActiveTap(null);
      if (current.targetApp) {
        setCurrentApp(current.targetApp);
      }

      if (current.action === 'finish') {
        setAgentState('FINISHED');
        setCurrentThought('Task completed.');
        confetti({ particleCount: 60, spread: 70, origin: { y: 0.6 } });
      } else {
        runStep(allSteps, index + 1);
      }
    }, executionSpeed * 0.8);
  };

  const handleEmergencyStop = () => {
    if (executionTimerRef.current) clearTimeout(executionTimerRef.current);
    setActiveTap(null);
    setAgentState('CANCELLED');
    setCurrentThought('Agent execution halted by user.');
    speakText("Agent stopped.");
  };

  const handleReset = () => {
    if (executionTimerRef.current) clearTimeout(executionTimerRef.current);
    setActiveTap(null);
    setTypedText('');
    setAgentState('IDLE');
    setCurrentThought('');
    setCurrentGoal('');
    setSteps([]);
    setCurrentStepIndex(-1);
    setCurrentApp('home');
    setConfirmationRequired(null);
  };

  return (
    <div className="min-h-screen bg-[#080C14] text-slate-100 flex flex-col">
      {/* Top Navigation Bar */}
      <header className="bg-[#0E1524] border-b border-slate-800/80 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 to-indigo-400 flex items-center justify-center text-white shadow-lg shadow-indigo-500/30">
              <Bot className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="font-bold text-base text-white tracking-tight">OmniAgent OS</h1>
                <span className="text-[10px] bg-indigo-500/20 text-indigo-300 font-mono font-semibold px-2 py-0.5 rounded-full border border-indigo-500/30">
                  Android Autonomous Assistant
                </span>
              </div>
              <p className="text-xs text-slate-400">Complete tasks across any Android app via simple voice commands</p>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="flex items-center space-x-1 bg-slate-900 p-1 rounded-xl border border-slate-800">
            <button
              onClick={() => setActiveTab('phone')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-all ${
                activeTab === 'phone' ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Virtual Phone & Agent</span>
            </button>
            <button
              onClick={() => setActiveTab('inspector')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-all ${
                activeTab === 'inspector' ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
              }`}
            >
              <Layers className="w-3.5 h-3.5" />
              <span>Accessibility Tree</span>
            </button>
            <button
              onClick={() => setActiveTab('code')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-all ${
                activeTab === 'code' ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
              }`}
            >
              <FileCode className="w-3.5 h-3.5" />
              <span>Android Kotlin Codebase</span>
            </button>
            <button
              onClick={() => setActiveTab('docs')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-all ${
                activeTab === 'docs' ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
              }`}
            >
              <HelpCircle className="w-3.5 h-3.5" />
              <span>Architecture & Guide</span>
            </button>
          </div>

          {/* Action buttons */}
          <div className="flex items-center space-x-2">
            <a
              href="/OmniAgent-v1.0.apk"
              download="OmniAgent-v1.0.apk"
              className="px-3.5 py-1.5 bg-gradient-to-r from-emerald-600 to-teal-500 hover:from-emerald-500 hover:to-teal-400 text-white font-bold text-xs rounded-xl flex items-center space-x-1.5 shadow-md shadow-emerald-900/30 transition-all hover:scale-105 active:scale-95"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Download APK</span>
            </a>

            <button
              onClick={() => setIsSettingsOpen(true)}
              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-xl border border-slate-700 flex items-center space-x-1.5 shadow-sm"
            >
              <Settings2 className="w-3.5 h-3.5 text-indigo-400" />
              <span>LLM ({model})</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Workspace Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-6">
        {activeTab === 'phone' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Left Column: Interactive Phone Mockup */}
            <div className="lg:col-span-5 flex flex-col items-center">
              <div className="w-full max-w-[360px]">
                <PhoneMockup
                  currentApp={currentApp}
                  setCurrentApp={setCurrentApp}
                  agentState={agentState}
                  currentThought={currentThought}
                  activeTap={activeTap}
                  showInspector={showInspector}
                  typedText={typedText}
                  isDarkMode={isDarkMode}
                  setIsDarkMode={setIsDarkMode}
                  onEmergencyStop={handleEmergencyStop}
                  onStartListening={() => setIsListening(true)}
                />
              </div>
            </div>

            {/* Right Column: Voice Control & Live ReAct Thought Stream */}
            <div className="lg:col-span-7 space-y-5">
              <VoiceController
                onSelectCommand={executeCommand}
                onRunCustomCommand={executeCommand}
                isListening={isListening}
                setIsListening={setIsListening}
                agentState={agentState}
                lastSpokenText={lastSpokenText}
              />

              <AgentReasoningStream
                steps={steps}
                currentStepIndex={currentStepIndex}
                agentState={agentState}
                goal={currentGoal}
                executionSpeed={executionSpeed}
                setExecutionSpeed={setExecutionSpeed}
                isPaused={isPaused}
                setIsPaused={setIsPaused}
                onReset={handleReset}
                confirmationRequired={confirmationRequired}
                onAuthorizeConfirmation={(approved) => {
                  if (approved && confirmationRequired?.onConfirm) {
                    confirmationRequired.onConfirm();
                  } else {
                    setConfirmationRequired(null);
                    handleEmergencyStop();
                  }
                }}
              />
            </div>
          </div>
        )}

        {activeTab === 'inspector' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            <div className="lg:col-span-4 flex justify-center">
              <div className="w-full max-w-[340px]">
                <PhoneMockup
                  currentApp={currentApp}
                  setCurrentApp={setCurrentApp}
                  agentState={agentState}
                  currentThought={currentThought}
                  activeTap={activeTap}
                  showInspector={showInspector}
                  typedText={typedText}
                  isDarkMode={isDarkMode}
                  setIsDarkMode={setIsDarkMode}
                  onEmergencyStop={handleEmergencyStop}
                  onStartListening={() => setIsListening(true)}
                />
              </div>
            </div>
            <div className="lg:col-span-8">
              <AccessibilityTreeViewer
                currentApp={currentApp}
                showInspector={showInspector}
                setShowInspector={setShowInspector}
              />
            </div>
          </div>
        )}

        {activeTab === 'code' && (
          <div>
            <CodeHub />
          </div>
        )}

        {activeTab === 'docs' && (
          <div>
            <DocsHub />
          </div>
        )}
      </main>

      {/* Settings Modal */}
      <ApiKeyModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        provider={provider}
        setProvider={setProvider}
        apiKey={apiKey}
        setApiKey={setApiKey}
        model={model}
        setModel={setModel}
      />
    </div>
  );
}
