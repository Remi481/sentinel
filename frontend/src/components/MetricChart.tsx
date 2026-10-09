import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { EMPTY_VALUE } from '../format'

export interface ChartPoint {
  t: number
  v: number | null
  v2?: number | null
}

interface SeriesConfig {
  key: 'v' | 'v2'
  label: string
  color: string
}

interface MetricChartProps {
  title: string
  points: ChartPoint[]
  series: SeriesConfig[]
  formatter: (value: number) => string
  max?: number
}

interface TooltipEntry {
  color?: string
  name?: string
  value?: number | string | null
}

interface ChartTooltipProps {
  active?: boolean
  payload?: ReadonlyArray<TooltipEntry>
  label?: number | string
  formatter: (value: number) => string
}

function ChartTooltip({ active, payload, label, formatter }: ChartTooltipProps) {
  if (!active || !payload || payload.length === 0) return null
  return (
    <div className="rounded-lg border border-neutral-200 bg-white p-2 text-xs text-neutral-700 shadow-md dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200">
      <p className="mb-1 font-medium text-neutral-900 dark:text-neutral-50">
        {new Date(Number(label)).toLocaleString()}
      </p>
      <ul className="space-y-0.5">
        {payload.map((entry) => (
          <li key={String(entry.name)} className="flex items-center gap-2">
            <span
              className="inline-block h-2 w-2 shrink-0 rounded-full"
              style={{ backgroundColor: entry.color }}
            />
            <span>{entry.name}</span>
            <span className="ml-auto pl-4 font-medium">
              {entry.value == null || Number.isNaN(Number(entry.value))
                ? EMPTY_VALUE
                : formatter(Number(entry.value))}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default function MetricChart({
  title,
  points,
  series,
  formatter,
  max,
}: MetricChartProps) {
  const hasData = points.some((point) =>
    series.some(
      (s) => point[s.key] != null && !Number.isNaN(Number(point[s.key])),
    ),
  )

  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-4 dark:border-neutral-800 dark:bg-neutral-900">
      <h3 className="mb-2 text-sm font-medium text-neutral-500 dark:text-neutral-400">
        {title}
      </h3>
      {hasData ? (
        <ResponsiveContainer width="100%" height={220}>
          <LineChart
            data={points}
            margin={{ top: 4, right: 8, bottom: 0, left: 0 }}
          >
            <CartesianGrid
              strokeDasharray="3 3"
              strokeOpacity={0.6}
              className="text-neutral-200 dark:text-neutral-800"
            />
            <XAxis
              type="number"
              dataKey="t"
              domain={['dataMin', 'dataMax']}
              tickFormatter={(value: number) =>
                new Date(value).toLocaleTimeString([], {
                  hour: '2-digit',
                  minute: '2-digit',
                })
              }
              tick={{ fontSize: 11 }}
              className="text-neutral-400 dark:text-neutral-500"
            />
            <YAxis
              domain={max != null ? [0, max] : [0, 'auto']}
              tickFormatter={formatter}
              width={72}
              tick={{ fontSize: 11 }}
              className="text-neutral-400 dark:text-neutral-500"
            />
            <Tooltip
              content={<ChartTooltip formatter={formatter} />}
              cursor={{ stroke: '#a1a1aa', strokeDasharray: '3 3' }}
            />
            {series.map((s) => (
              <Line
                key={s.key}
                type="monotone"
                dataKey={s.key}
                name={s.label}
                stroke={s.color}
                strokeWidth={2}
                dot={false}
                connectNulls={false}
                isAnimationActive={false}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      ) : (
        <div className="flex h-[220px] items-center justify-center text-sm text-neutral-400 dark:text-neutral-500">
          No data yet
        </div>
      )}
    </div>
  )
}