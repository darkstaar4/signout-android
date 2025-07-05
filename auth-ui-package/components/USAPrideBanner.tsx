import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useTheme } from '@/context/ThemeContext';

type USAPrideBannerProps = {
  compact?: boolean;
  forceLightTheme?: boolean;
};

export default function USAPrideBanner({ compact = false, forceLightTheme = false }: USAPrideBannerProps) {
  const { isDark } = useTheme();
  // If forceLightTheme is true, always use light mode
  const shouldUseLightTheme = forceLightTheme ? true : !(!forceLightTheme && isDark);

  return (
    <View style={[
      styles.container,
      !shouldUseLightTheme && styles.containerDark,
      compact && styles.containerCompact
    ]}>
      <Text style={styles.flag}>🇺🇸</Text>
      <View style={styles.textContainer}>
        <Text style={[
          styles.title,
          !shouldUseLightTheme && styles.titleDark,
          styles.centerText
        ]}>
          Designed & Made in the USA
        </Text>
        <Text style={[
          styles.subtitle,
          !shouldUseLightTheme && styles.subtitleDark,
          styles.centerText
        ]}>
          Your data is protected by US technology and servers
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F0F9FF',
    padding: 8,
    borderRadius: 8,
    marginTop: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#E0F2FE',
    shadowColor: '#0EA5E9',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
    minHeight: 40,
    justifyContent: 'center',
  },
  containerDark: {
    backgroundColor: '#1E293B',
    borderColor: '#334155',
  },
  containerCompact: {
    padding: 4,
    marginTop: 4,
    marginBottom: 4,
    minHeight: 32,
  },
  flag: {
    fontSize: 18,
    marginRight: 8,
  },
  textContainer: {
    flex: 1,
    alignItems: 'center',
  },
  title: {
    fontSize: 13,
    fontWeight: '700',
    color: '#0EA5E9',
    marginBottom: 0,
    textAlign: 'center',
  },
  titleDark: {
    color: '#7DD3FC',
  },
  subtitle: {
    fontSize: 11,
    color: '#64748B',
    lineHeight: 15,
    textAlign: 'center',
  },
  subtitleDark: {
    color: '#94A3B8',
  },
  centerText: {
    textAlign: 'center',
  },
}); 