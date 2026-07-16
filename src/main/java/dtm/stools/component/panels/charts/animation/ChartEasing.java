package dtm.stools.component.panels.charts.animation;

public enum ChartEasing {

    LINEAR {
        @Override
        public float apply(float t) {
            return t;
        }
    },
    EASE_IN_CUBIC {
        @Override
        public float apply(float t) {
            return t * t * t;
        }
    },
    EASE_OUT_CUBIC {
        @Override
        public float apply(float t) {
            float inv = 1f - t;
            return 1f - inv * inv * inv;
        }
    },
    EASE_IN_OUT_CUBIC {
        @Override
        public float apply(float t) {
            if (t < 0.5f) return 4f * t * t * t;
            float inv = -2f * t + 2f;
            return 1f - inv * inv * inv / 2f;
        }
    },
    EASE_OUT_QUINT {
        @Override
        public float apply(float t) {
            float inv = 1f - t;
            return 1f - inv * inv * inv * inv * inv;
        }
    },
    EASE_OUT_BACK {
        @Override
        public float apply(float t) {
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            float inv = t - 1f;
            return 1f + c3 * inv * inv * inv + c1 * inv * inv;
        }
    },
    EASE_OUT_ELASTIC {
        @Override
        public float apply(float t) {
            if (t <= 0f) return 0f;
            if (t >= 1f) return 1f;
            float c4 = (float) (2 * Math.PI / 3.0);
            return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1);
        }
    },
    EASE_OUT_BOUNCE {
        @Override
        public float apply(float t) {
            float n1 = 7.5625f;
            float d1 = 2.75f;
            if (t < 1f / d1) {
                return n1 * t * t;
            } else if (t < 2f / d1) {
                t -= 1.5f / d1;
                return n1 * t * t + 0.75f;
            } else if (t < 2.5f / d1) {
                t -= 2.25f / d1;
                return n1 * t * t + 0.9375f;
            }
            t -= 2.625f / d1;
            return n1 * t * t + 0.984375f;
        }
    };

    public abstract float apply(float t);

    public float applyClamped(float t) {
        if (t <= 0f) return apply(0f);
        if (t >= 1f) return apply(1f);
        return apply(t);
    }
}
