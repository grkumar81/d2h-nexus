import type { UploadResult } from '../types'
import styles from './UploadResultPanel.module.css'

export default function UploadResultPanel({ result }: { result: UploadResult }) {
  return (
    <div className={styles.panel}>
      <p>
        <strong>Processed:</strong> {result.successCount} / {result.totalRows} rows
        {result.totalAmountProcessed !== undefined && (
          <> — Amount: ₹{result.totalAmountProcessed.toLocaleString()}</>
        )}
      </p>
      {result.failureCount > 0 && (
        <details>
          <summary>{result.failureCount} error(s)</summary>
          <ul className={styles.errors}>
            {result.errors.map((e, i) => <li key={i}>{e}</li>)}
          </ul>
        </details>
      )}
    </div>
  )
}
