export function EmptyPreview() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-3 rounded-xl bg-slate-50 px-6 py-12 text-center">
      {/* Icon circle */}
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-slate-200">
        <span className="text-2xl text-slate-400" aria-hidden="true">
          ✉
        </span>
      </div>

      {/* Title */}
      <p className="text-[16px] font-medium text-slate-500">미리보기</p>

      {/* Description */}
      <p className="text-[13px] leading-relaxed text-slate-400">
        수신자와 본문을 입력하면
        <br />
        메시지가 여기에 표시됩니다.
      </p>

      {/* Channel hint chips */}
      <div className="mt-2 flex items-center gap-2">
        {['SMS', '알림톡', 'RCS'].map((label) => (
          <span
            key={label}
            className="rounded-full border border-slate-200 bg-white px-4 py-1 text-[12px] text-slate-500"
          >
            {label}
          </span>
        ))}
      </div>
      <p className="text-[11px] text-slate-400">채널을 선택하여 미리보기 형식을 변경할 수 있습니다.</p>
    </div>
  );
}
