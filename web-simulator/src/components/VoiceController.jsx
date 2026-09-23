import React, { useState, useEffect, useRef } from 'react';
import { Mic, MicOff, Send, Sparkles, Volume2, Play, AlertCircle } from 'lucide-react';
import { SAMPLE_COMMANDS } from '../data/mockApps';

export default function VoiceController({
  onSelectCommand,
  onRunCustomCommand,
  isListening,
  setIsListening,
  agentState,
  lastSpokenText
}) {
  const [inputText, setInputText] = useState('');
  const [browserSpeechSupported, setBrowserSpeechSupported] = useState(false);
  const recognitionRef = useRef(null);

  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      setBrowserSpeechSupported(true);
      const recognizer = new SpeechRecognition();
      recognizer.continuous = false;
      recognizer.interimResults = false;
      recognizer.lang = 'en-US';

      recognizer.onstart = () => {
        setIsListening(true);
      };

      recognizer.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        setIsListening(false);
        setInputText(transcript);
        onRunCustomCommand(transcript);
      };

      recognizer.onerror = () => {
        setIsListening(false);
      };

      recognizer.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognizer;
    }
  }, [onRunCustomCommand, setIsListening]);

  const toggleMic = () => {
    if (isListening) {
      if (recognitionRef.current) recognitionRef.current.stop();
      setIsListening(false);
    } else {
      if (browserSpeechSupported && recognitionRef.current) {
        try {
          recognitionRef.current.start();
        } catch {
          // Fallback simulation
          setIsListening(true);
          setTimeout(() => {
            setIsListening(false);
            const sample = "Send a message to Alex on WhatsApp saying I'll be 10m late";
            setInputText(sample);
            onRunCustomCommand(sample);
          }, 2500);
        }
      } else {
        // Speech API not supported in this browser -> simulate 2s listening
        setIsListening(true);
        setTimeout(() => {
          setIsListening(false);
          const sample = "Order a Margherita pizza on DoorDash";
          setInputText(sample);
          onRunCustomCommand(sample);
        }, 2200);
      }
    }
  };

  const handleCustomSubmit = (e) => {
    e.preventDefault();
    if (inputText.trim()) {
      onRunCustomCommand(inputText.trim());
    }
  };

  return (
    <div className="bg-agent-card rounded-2xl p-5 border border-agent-border shadow-xl space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
            <Mic className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-bold text-sm text-white">Voice Command Input</h3>
            <p className="text-xs text-slate-400">Speak naturally to trigger any action across any app</p>
          </div>
        </div>

        {lastSpokenText && (
          <div className="flex items-center space-x-1.5 px-3 py-1 bg-emerald-500/10 border border-emerald-500/30 rounded-full text-emerald-400 text-xs">
            <Volume2 className="w-3.5 h-3.5 animate-pulse" />
            <span className="font-mono text-[11px] truncate max-w-[200px]">TTS: "{lastSpokenText}"</span>
          </div>
        )}
      </div>

      {/* Main Mic Button & Waveform Container */}
      <div className="flex items-center space-x-4 bg-slate-900/60 p-3.5 rounded-xl border border-slate-800">
        <button
          onClick={toggleMic}
          className={`w-14 h-14 rounded-2xl flex items-center justify-center transition-all shadow-lg shrink-0 ${
            isListening 
              ? 'bg-rose-500 text-white animate-pulse ring-4 ring-rose-500/30' 
              : 'bg-indigo-600 hover:bg-indigo-500 text-white hover:scale-105 active:scale-95'
          }`}
        >
          {isListening ? <MicOff className="w-6 h-6" /> : <Mic className="w-6 h-6" />}
        </button>

        <div className="flex-1">
          {isListening ? (
            <div className="space-y-1.5">
              <div className="flex items-center space-x-1 text-rose-400 text-xs font-semibold">
                <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping"></span>
                <span>Listening for speech... (Say your command)</span>
              </div>
              {/* Animated Audio Waveform */}
              <div className="flex items-center space-x-1 h-6">
                {[12, 24, 16, 28, 8, 20, 32, 14, 22, 10, 26, 18].map((h, i) => (
                  <div
                    key={i}
                    className="w-1 bg-rose-400 rounded-full animate-wave"
                    style={{ animationDelay: `${i * 0.1}s`, height: `${h}px` }}
                  ></div>
                ))}
              </div>
            </div>
          ) : (
            <div>
              <div className="text-xs font-semibold text-slate-200">
                {agentState === 'EXECUTING' ? 'Agent actively controlling phone...' : 'Tap the microphone or select a prompt below'}
              </div>
              <div className="text-[11px] text-slate-400 mt-0.5">
                Uses Android Foreground Service + SpeechRecognizer in the native APK
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Quick Voice Command Chips */}
      <div className="space-y-2">
        <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
          One-Tap Voice Command Presets
        </span>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          {SAMPLE_COMMANDS.map((item) => (
            <button
              key={item.id}
              onClick={() => {
                setInputText(item.command);
                onSelectCommand(item);
              }}
              className="text-left p-2.5 rounded-xl bg-slate-800/70 hover:bg-slate-700/80 border border-slate-700/60 hover:border-indigo-500/50 transition-all flex items-start space-x-2.5 group"
            >
              <div 
                className="w-7 h-7 rounded-lg flex items-center justify-center shrink-0 mt-0.5"
                style={{ backgroundColor: `${item.color}22`, color: item.color }}
              >
                <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
              </div>
              <div className="min-w-0">
                <div className="text-xs font-semibold text-slate-200 group-hover:text-white">
                  {item.title}
                </div>
                <div className="text-[11px] text-slate-400 truncate">
                  "{item.command}"
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Custom Command Input */}
      <form onSubmit={handleCustomSubmit} className="flex items-center space-x-2">
        <input
          type="text"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="Type any command (e.g., 'Book a flight to Tokyo on Expedia')..."
          className="flex-1 bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
        />
        <button
          type="submit"
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs rounded-xl flex items-center space-x-1 shadow-md shrink-0"
        >
          <Sparkles className="w-3.5 h-3.5" />
          <span>Execute</span>
        </button>
      </form>
    </div>
  );
}
