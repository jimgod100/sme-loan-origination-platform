import React from 'react';

const steps = [
  'Business & contact details',
  'Financial information',
  'Document upload',
  'Consent & review',
];

const App: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center py-10">
      <div className="w-full max-w-2xl bg-white shadow-sm rounded-lg p-6">
        <h1 className="text-2xl font-semibold mb-4 text-slate-900">
          SME Loan Customer Onboarding
        </h1>
        <p className="text-sm text-slate-600 mb-6">
          This wizard will guide you through four short steps to verify your identity, provide
          financial information, upload supporting documents, and confirm consent.
        </p>

        <ol className="space-y-3 mb-8">
          {steps.map((label, index) => (
            <li key={label} className="flex items-start">
              <span className="mt-0.5 mr-3 flex h-6 w-6 items-center justify-center rounded-full bg-slate-900 text-xs font-semibold text-white">
                {index + 1}
              </span>
              <span className="text-sm text-slate-800">{label}</span>
            </li>
          ))}
        </ol>

        <div className="rounded-md border border-dashed border-slate-300 bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">
          In the next iteration, this screen will be wired to the backend onboarding token API and
          render the full multi-step form with validation.
        </div>
      </div>
    </div>
  );
};

export default App;
