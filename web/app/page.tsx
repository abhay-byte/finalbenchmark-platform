import AppBar from '../components/AppBar'

export default function Home() {
  return (
    <main className="min-h-screen">
      <AppBar />
      
      {/* Placeholder content to enable scrolling */}
      <div className="pt-16">
        {/* Hero Section */}
        <section className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100">
          <div className="max-w-7xl mx-auto px-4 text-center">
            <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6">
              FinalBenchmark2
            </h1>
            <p className="text-xl md:text-2xl text-gray-600 mb-8 max-w-3xl mx-auto">
              Professional CPU Benchmarking for Android
            </p>
            <p className="text-lg text-gray-500 mb-12 max-w-2xl mx-auto">
              Comprehensive CPU benchmarking application with real-time testing, detailed reports, and performance analysis.
            </p>
            <div className="text-mascot-primary font-semibold text-lg">
              Phase I: AppBar Implementation Complete
            </div>
          </div>
        </section>

        {/* Scrollable Content to Test AppBar */}
        <section className="py-20 bg-white">
          <div className="max-w-7xl mx-auto px-4">
            <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
              Scroll to test AppBar sticky behavior
            </h2>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="bg-gray-50 rounded-lg p-6 h-48 flex items-center justify-center">
                  <div className="text-gray-400 text-lg">Feature {i}</div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="py-20 bg-gray-50">
          <div className="max-w-7xl mx-auto px-4">
            <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
              More Content Below
            </h2>
            <div className="space-y-12">
              {[1, 2, 3].map((i) => (
                <div key={i} className="bg-white rounded-lg p-8 shadow-sm">
                  <h3 className="text-2xl font-semibold text-gray-900 mb-4">
                    Section {i}
                  </h3>
                  <p className="text-gray-600 leading-relaxed">
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor 
                    incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis 
                    nostrud exercitation ullamco laboris.
                  </p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="py-20 bg-white">
          <div className="max-w-7xl mx-auto px-4">
            <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
              Bottom Section
            </h2>
            <div className="text-center">
              <p className="text-gray-600 mb-8">
                This is the end of the page. The AppBar should have maintained its sticky position throughout scrolling.
              </p>
              <div className="inline-block bg-mascot-primary text-white px-8 py-4 rounded-lg font-semibold">
                AppBar Test Complete ✓
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  )
}
